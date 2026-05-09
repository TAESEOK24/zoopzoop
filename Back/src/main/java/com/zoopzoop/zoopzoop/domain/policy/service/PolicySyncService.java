package com.zoopzoop.zoopzoop.domain.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyConditionsDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyDetailDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyListDto;
import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySyncService {

    private static final int JDBC_BATCH_SIZE = 1000;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private volatile boolean upsertIndexesReady;

    @Value("${gov.api.key}")
    private String serviceKey;

    @Value("${policy.sync.full.per-page:100}")
    private int fullSyncPerPage;

    @Value("${policy.sync.full.max-pages:150}")
    private int fullSyncMaxPages;

    @Value("${policy.sync.full.page-delay-millis:500}")
    private long fullSyncPageDelayMillis;

    @Value("${policy.sync.api.max-attempts:3}")
    private int apiMaxAttempts;

    @Value("${policy.sync.api.retry-delay-millis:1000}")
    private long apiRetryDelayMillis;

    private static final String LIST_URL = "https://api.odcloud.kr/api/gov24/v3/serviceList";
    private static final String DETAIL_URL = "https://api.odcloud.kr/api/gov24/v3/serviceDetail";
    private static final String COND_URL = "https://api.odcloud.kr/api/gov24/v3/supportConditions";

    private static final String LIST_UPSERT_SQL = """
            insert into policies_list (
                service_id, service_type, service_name, purpose_summary, target, selection_criteria,
                support_content, application_method, application_deadline, detail_url, org_code, org_name,
                department_name, view_count, org_type, user_type, service_field, receiving_org, contact_number,
                created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (service_id) do update set
                service_type = excluded.service_type,
                service_name = excluded.service_name,
                purpose_summary = excluded.purpose_summary,
                target = excluded.target,
                selection_criteria = excluded.selection_criteria,
                support_content = excluded.support_content,
                application_method = excluded.application_method,
                application_deadline = excluded.application_deadline,
                detail_url = excluded.detail_url,
                org_code = excluded.org_code,
                org_name = excluded.org_name,
                department_name = excluded.department_name,
                view_count = excluded.view_count,
                org_type = excluded.org_type,
                user_type = excluded.user_type,
                service_field = excluded.service_field,
                receiving_org = excluded.receiving_org,
                contact_number = excluded.contact_number,
                created_at = excluded.created_at,
                updated_at = excluded.updated_at
            """;

    private static final String DETAIL_UPSERT_SQL = """
            insert into policies_detail (
                service_id, purpose, required_documents, receiving_org_name, contact_info, online_url,
                org_name, admin_rule, local_rule, law, official_required_docs, user_required_docs, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (service_id) do update set
                purpose = excluded.purpose,
                required_documents = excluded.required_documents,
                receiving_org_name = excluded.receiving_org_name,
                contact_info = excluded.contact_info,
                online_url = excluded.online_url,
                org_name = excluded.org_name,
                admin_rule = excluded.admin_rule,
                local_rule = excluded.local_rule,
                law = excluded.law,
                official_required_docs = excluded.official_required_docs,
                user_required_docs = excluded.user_required_docs,
                updated_at = excluded.updated_at
            """;

    private static final String CONDITIONS_UPSERT_SQL = """
            insert into policies_conditions (
                service_id, service_name, ja0110, ja0111, ja0101, ja0102, ja0201, ja0202, ja0203, ja0204,
                ja0205, ja0301, ja0302, ja0303, ja0313, ja0314, ja0315, ja0316, ja0317, ja0318,
                ja0319, ja0320, ja0322, ja0326, ja0327, ja0401, ja0402, ja0403, ja0404, ja0410,
                ja0411, ja0412, ja0413, ja0414, ja1101, ja1102, ja1103, ja1201, ja1202, ja1299,
                ja2101, ja2102, ja2103, ja2201, ja2202, ja2203, ja2299, ja0328, ja0329, ja0330
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (service_id) do update set
                service_name = excluded.service_name,
                ja0110 = excluded.ja0110,
                ja0111 = excluded.ja0111,
                ja0101 = excluded.ja0101,
                ja0102 = excluded.ja0102,
                ja0201 = excluded.ja0201,
                ja0202 = excluded.ja0202,
                ja0203 = excluded.ja0203,
                ja0204 = excluded.ja0204,
                ja0205 = excluded.ja0205,
                ja0301 = excluded.ja0301,
                ja0302 = excluded.ja0302,
                ja0303 = excluded.ja0303,
                ja0313 = excluded.ja0313,
                ja0314 = excluded.ja0314,
                ja0315 = excluded.ja0315,
                ja0316 = excluded.ja0316,
                ja0317 = excluded.ja0317,
                ja0318 = excluded.ja0318,
                ja0319 = excluded.ja0319,
                ja0320 = excluded.ja0320,
                ja0322 = excluded.ja0322,
                ja0326 = excluded.ja0326,
                ja0327 = excluded.ja0327,
                ja0401 = excluded.ja0401,
                ja0402 = excluded.ja0402,
                ja0403 = excluded.ja0403,
                ja0404 = excluded.ja0404,
                ja0410 = excluded.ja0410,
                ja0411 = excluded.ja0411,
                ja0412 = excluded.ja0412,
                ja0413 = excluded.ja0413,
                ja0414 = excluded.ja0414,
                ja1101 = excluded.ja1101,
                ja1102 = excluded.ja1102,
                ja1103 = excluded.ja1103,
                ja1201 = excluded.ja1201,
                ja1202 = excluded.ja1202,
                ja1299 = excluded.ja1299,
                ja2101 = excluded.ja2101,
                ja2102 = excluded.ja2102,
                ja2103 = excluded.ja2103,
                ja2201 = excluded.ja2201,
                ja2202 = excluded.ja2202,
                ja2203 = excluded.ja2203,
                ja2299 = excluded.ja2299,
                ja0328 = excluded.ja0328,
                ja0329 = excluded.ja0329,
                ja0330 = excluded.ja0330
            """;

    public String syncFullData() {
        int page = 1;
        int totalListCount = 0;
        int totalDetailCount = 0;
        int totalConditionCount = 0;
        Integer totalPages = null;
        Integer expectedTotalCount = null;

        log.info("Starting full policy sync");

        while (true) {
            try {
                log.info("Syncing page {}", page);

                SyncPageResult result = fetchAndSaveAll(page, fullSyncPerPage);
                if (!result.success()) {
                    log.error("Stopping full sync at page {}: {}", page, result.message());
                    return "FAILED: stopped at page " + page
                            + " after processing list=" + totalListCount
                            + ", detail=" + totalDetailCount
                            + ", conditions=" + totalConditionCount
                            + ". cause=" + result.message();
                }
                if (totalPages == null && result.listTotalCount() != null) {
                    expectedTotalCount = result.listTotalCount();
                    totalPages = Math.max(1, (int) Math.ceil((double) result.listTotalCount() / fullSyncPerPage));
                    if (totalPages > fullSyncMaxPages) {
                        log.warn("Policy sync total pages {} exceeds configured max pages {}. Capping at configured max.",
                                totalPages, fullSyncMaxPages);
                        totalPages = fullSyncMaxPages;
                    }
                    log.info("Policy sync total count={}, total pages={}, perPage={}",
                            result.listTotalCount(), totalPages, fullSyncPerPage);
                }
                if (result.isEmpty()) {
                    log.info("Stopping full sync because page {} returned no policy data.", page);
                    break;
                }

                totalListCount += result.listCount();
                totalDetailCount += result.detailCount();
                totalConditionCount += result.conditionCount();
                page++;
                Thread.sleep(fullSyncPageDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Policy sync interrupted at page {}", page);
                return "FAILED: sync interrupted at page " + page;
            } catch (Exception e) {
                log.error("Unexpected error during full policy sync", e);
                return "FAILED: sync aborted at page " + page;
            }

            int lastPage = totalPages != null ? totalPages : fullSyncMaxPages;
            if (page > lastPage) {
                break;
            }
        }

        log.info("Finished full policy sync. expectedTotal={}, list={}, detail={}, conditions={}, pages={}",
                expectedTotalCount, totalListCount, totalDetailCount, totalConditionCount, page - 1);
        return "SUCCESS: synced through page " + (page - 1)
                + ", expectedTotal=" + valueOrUnknown(expectedTotalCount)
                + ", list=" + totalListCount
                + ", detail=" + totalDetailCount
                + ", conditions=" + totalConditionCount;
    }

    @Transactional
    public SyncPageResult fetchAndSaveAll(int page, int perPage) {
        try {
            ensureUpsertIndexes();

            ApiPage<PolicyListDto> listPage = fetchFromApi(LIST_URL, page, perPage, PolicyListDto.class);
            saveList(listPage.items());

            ApiPage<PolicyDetailDto> detailPage = fetchFromApi(DETAIL_URL, page, perPage, PolicyDetailDto.class);
            saveDetail(detailPage.items());

            ApiPage<PolicyConditionsDto> conditionPage = fetchFromApi(COND_URL, page, perPage, PolicyConditionsDto.class);
            saveConditions(conditionPage.items());

            return SyncPageResult.success(
                    listPage.items().size(),
                    detailPage.items().size(),
                    conditionPage.items().size(),
                    listPage.totalCount()
            );
        } catch (Exception e) {
            log.error("Policy sync failed", e);
            return SyncPageResult.failure("FAILED: " + e.getMessage());
        }
    }

    private void ensureUpsertIndexes() {
        if (upsertIndexesReady) {
            return;
        }

        synchronized (this) {
            if (upsertIndexesReady) {
                return;
            }

            jdbcTemplate.execute("create unique index if not exists policies_list_service_id_uidx on policies_list (service_id)");
            jdbcTemplate.execute("create unique index if not exists policies_detail_service_id_uidx on policies_detail (service_id)");
            jdbcTemplate.execute("create unique index if not exists policies_conditions_service_id_uidx on policies_conditions (service_id)");
            upsertIndexesReady = true;
        }
    }

    private <T> ApiPage<T> fetchFromApi(String baseUrl, int page, int perPage, Class<T> clazz) throws Exception {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("page", page)
                .queryParam("perPage", perPage)
                .queryParam("serviceKey", serviceKey)
                .build(true)
                .toUri();

        String response = fetchWithRetry(uri, baseUrl, page);
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode dataNode = rootNode.path("data");

        List<T> dtos = new ArrayList<>();
        if (dataNode.isArray()) {
            for (JsonNode node : dataNode) {
                dtos.add(objectMapper.treeToValue(node, clazz));
            }
        }
        Integer totalCount = rootNode.hasNonNull("totalCount") ? rootNode.path("totalCount").asInt() : null;
        return new ApiPage<>(dtos, totalCount);
    }

    private String fetchWithRetry(URI uri, String baseUrl, int page) throws InterruptedException {
        int attempts = Math.max(1, apiMaxAttempts);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return restTemplate.getForObject(uri, String.class);
            } catch (HttpClientErrorException.BadRequest e) {
                if (!isRetryableUnknownError(e) || attempt == attempts) {
                    throw e;
                }
                log.warn("Retrying policy API request after UNKNOWN response. endpoint={}, page={}, attempt={}/{}",
                        baseUrl, page, attempt, attempts);
                Thread.sleep(apiRetryDelayMillis);
            }
        }

        throw new IllegalStateException("Policy API request failed without response. endpoint=" + baseUrl + ", page=" + page);
    }

    private boolean isRetryableUnknownError(HttpClientErrorException.BadRequest e) {
        String body = e.getResponseBodyAsString();
        return body != null && body.contains("\"code\":-999") && body.contains("\"msg\":\"UNKNOWN\"");
    }

    private String valueOrUnknown(Integer value) {
        return value == null ? "unknown" : value.toString();
    }

    private void saveList(List<PolicyListDto> dtos) {
        List<PolicyListDto> validDtos = dtos.stream()
                .filter(dto -> dto.serviceId() != null)
                .toList();

        jdbcTemplate.batchUpdate(LIST_UPSERT_SQL, validDtos, JDBC_BATCH_SIZE, this::bindList);
    }

    private void saveDetail(List<PolicyDetailDto> dtos) {
        List<PolicyDetailDto> validDtos = dtos.stream()
                .filter(dto -> dto.serviceId() != null)
                .toList();

        jdbcTemplate.batchUpdate(DETAIL_UPSERT_SQL, validDtos, JDBC_BATCH_SIZE, this::bindDetail);
    }

    private void saveConditions(List<PolicyConditionsDto> dtos) {
        List<PolicyConditionsDto> validDtos = dtos.stream()
                .filter(dto -> dto.serviceId() != null)
                .toList();

        jdbcTemplate.batchUpdate(CONDITIONS_UPSERT_SQL, validDtos, JDBC_BATCH_SIZE, this::bindConditions);
    }

    private void bindList(PreparedStatement ps, PolicyListDto dto) throws SQLException {
        int index = 1;
        ps.setString(index++, dto.serviceId());
        ps.setString(index++, defaultString(dto.serviceType(), "-"));
        ps.setString(index++, defaultString(dto.serviceName(), "N/A"));
        ps.setString(index++, dto.purposeSummary());
        ps.setString(index++, dto.target());
        ps.setString(index++, dto.selectionCriteria());
        ps.setString(index++, dto.supportContent());
        ps.setString(index++, dto.applicationMethod());
        ps.setString(index++, dto.applicationDeadline());
        ps.setString(index++, dto.detailUrl());
        ps.setString(index++, defaultString(dto.orgCode(), "-"));
        ps.setString(index++, defaultString(dto.orgName(), "-"));
        ps.setString(index++, dto.departmentName());
        setInteger(ps, index++, dto.viewCount() != null ? dto.viewCount() : 0);
        ps.setString(index++, dto.orgType());
        ps.setString(index++, dto.userType());
        ps.setString(index++, dto.serviceField());
        ps.setString(index++, dto.receivingOrg());
        ps.setString(index++, dto.contactNumber());
        setDateTime(ps, index++, parseDateTime(dto.createdAt()));
        setDateTime(ps, index, parseDateTime(dto.updatedAt()));
    }

    private void bindDetail(PreparedStatement ps, PolicyDetailDto dto) throws SQLException {
        int index = 1;
        ps.setString(index++, dto.serviceId());
        ps.setString(index++, defaultString(dto.purpose(), "-"));
        ps.setString(index++, dto.requiredDocuments());
        ps.setString(index++, dto.receivingOrgName());
        ps.setString(index++, dto.contactInfo());
        ps.setString(index++, dto.onlineUrl());
        ps.setString(index++, defaultString(dto.orgName(), "-"));
        ps.setString(index++, dto.adminRule());
        ps.setString(index++, dto.localRule());
        ps.setString(index++, dto.law());
        ps.setString(index++, dto.officialRequiredDocs());
        ps.setString(index++, dto.userRequiredDocs());
        setDateTime(ps, index, parseDateTime(dto.updatedAt()));
    }

    private void bindConditions(PreparedStatement ps, PolicyConditionsDto dto) throws SQLException {
        int index = 1;
        ps.setString(index++, dto.serviceId());
        ps.setString(index++, defaultString(dto.serviceName(), "-"));
        setInteger(ps, index++, dto.ja0110());
        setInteger(ps, index++, dto.ja0111());
        ps.setString(index++, dto.ja0101());
        ps.setString(index++, dto.ja0102());
        ps.setString(index++, dto.ja0201());
        ps.setString(index++, dto.ja0202());
        ps.setString(index++, dto.ja0203());
        ps.setString(index++, dto.ja0204());
        ps.setString(index++, dto.ja0205());
        ps.setString(index++, dto.ja0301());
        ps.setString(index++, dto.ja0302());
        ps.setString(index++, dto.ja0303());
        ps.setString(index++, dto.ja0313());
        ps.setString(index++, dto.ja0314());
        ps.setString(index++, dto.ja0315());
        ps.setString(index++, dto.ja0316());
        ps.setString(index++, dto.ja0317());
        ps.setString(index++, dto.ja0318());
        ps.setString(index++, dto.ja0319());
        ps.setString(index++, dto.ja0320());
        ps.setString(index++, dto.ja0322());
        ps.setString(index++, dto.ja0326());
        ps.setString(index++, dto.ja0327());
        ps.setString(index++, dto.ja0401());
        ps.setString(index++, dto.ja0402());
        ps.setString(index++, dto.ja0403());
        ps.setString(index++, dto.ja0404());
        ps.setString(index++, dto.ja0410());
        ps.setString(index++, dto.ja0411());
        ps.setString(index++, dto.ja0412());
        ps.setString(index++, dto.ja0413());
        ps.setString(index++, dto.ja0414());
        ps.setString(index++, dto.ja1101());
        ps.setString(index++, dto.ja1102());
        ps.setString(index++, dto.ja1103());
        ps.setString(index++, dto.ja1201());
        ps.setString(index++, dto.ja1202());
        ps.setString(index++, dto.ja1299());
        ps.setString(index++, dto.ja2101());
        ps.setString(index++, dto.ja2102());
        ps.setString(index++, dto.ja2103());
        ps.setString(index++, dto.ja2201());
        ps.setString(index++, dto.ja2202());
        ps.setString(index++, dto.ja2203());
        ps.setString(index++, dto.ja2299());
        ps.setString(index++, dto.ja0328());
        ps.setString(index++, dto.ja0329());
        ps.setString(index, dto.ja0330());
    }

    private void setInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
            return;
        }
        ps.setInt(index, value);
    }

    private void setDateTime(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        ps.setObject(index, value, Types.TIMESTAMP);
    }

    private String defaultString(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDateTime.now();
        }

        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    public record SyncPageResult(
            boolean success,
            int listCount,
            int detailCount,
            int conditionCount,
            Integer listTotalCount,
            String message
    ) {
        static SyncPageResult success(int listCount, int detailCount, int conditionCount, Integer listTotalCount) {
            return new SyncPageResult(true, listCount, detailCount, conditionCount, listTotalCount, "SUCCESS");
        }

        static SyncPageResult failure(String message) {
            return new SyncPageResult(false, 0, 0, 0, null, message);
        }

        boolean isEmpty() {
            return listCount == 0 && detailCount == 0 && conditionCount == 0;
        }
    }

    private record ApiPage<T>(
            List<T> items,
            Integer totalCount
    ) {
    }
}
