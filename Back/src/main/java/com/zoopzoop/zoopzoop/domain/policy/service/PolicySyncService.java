package com.zoopzoop.zoopzoop.domain.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyConditionsDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyDetailDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyListDto;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyConditions;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyDetail;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyConditionsRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyDetailRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySyncService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PolicyListRepository listRepo;
    private final PolicyDetailRepository detailRepo;
    private final PolicyConditionsRepository conditionsRepo;

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
        for (PolicyListDto dto : dtos) {
            if (dto.serviceId() == null) {
                continue;
            }

            listRepo.save(PolicyList.builder()
                    .serviceId(dto.serviceId())
                    .serviceType(dto.serviceType() != null ? dto.serviceType() : "-")
                    .serviceName(dto.serviceName() != null ? dto.serviceName() : "N/A")
                    .purposeSummary(dto.purposeSummary())
                    .target(dto.target())
                    .selectionCriteria(dto.selectionCriteria())
                    .supportContent(dto.supportContent())
                    .applicationMethod(dto.applicationMethod())
                    .applicationDeadline(dto.applicationDeadline())
                    .detailUrl(dto.detailUrl())
                    .orgCode(dto.orgCode() != null ? dto.orgCode() : "-")
                    .orgName(dto.orgName() != null ? dto.orgName() : "-")
                    .departmentName(dto.departmentName())
                    .viewCount(dto.viewCount() != null ? dto.viewCount() : 0)
                    .orgType(dto.orgType())
                    .userType(dto.userType())
                    .serviceField(dto.serviceField())
                    .receivingOrg(dto.receivingOrg())
                    .contactNumber(dto.contactNumber())
                    .createdAt(parseDateTime(dto.createdAt()))
                    .updatedAt(parseDateTime(dto.updatedAt()))
                    .build());
        }
    }

    private void saveDetail(List<PolicyDetailDto> dtos) {
        for (PolicyDetailDto dto : dtos) {
            if (dto.serviceId() == null) {
                continue;
            }
            if (!listRepo.existsById(dto.serviceId())) {
                continue;
            }

            detailRepo.save(PolicyDetail.builder()
                    .serviceId(dto.serviceId())
                    .purpose(dto.purpose() != null ? dto.purpose() : "-")
                    .requiredDocuments(dto.requiredDocuments())
                    .receivingOrgName(dto.receivingOrgName())
                    .contactInfo(dto.contactInfo())
                    .onlineUrl(dto.onlineUrl())
                    .orgName(dto.orgName() != null ? dto.orgName() : "-")
                    .adminRule(dto.adminRule())
                    .localRule(dto.localRule())
                    .law(dto.law())
                    .officialRequiredDocs(dto.officialRequiredDocs())
                    .userRequiredDocs(dto.userRequiredDocs())
                    .updatedAt(parseDateTime(dto.updatedAt()))
                    .build());
        }
    }

    private void saveConditions(List<PolicyConditionsDto> dtos) {
        for (PolicyConditionsDto dto : dtos) {
            if (dto.serviceId() == null) {
                continue;
            }
            if (!listRepo.existsById(dto.serviceId())) {
                continue;
            }

            conditionsRepo.save(PolicyConditions.builder()
                    .serviceId(dto.serviceId())
                    .serviceName(dto.serviceName() != null ? dto.serviceName() : "-")
                    .ja0110(dto.ja0110())
                    .ja0111(dto.ja0111())
                    .ja0101(dto.ja0101())
                    .ja0102(dto.ja0102())
                    .ja0201(dto.ja0201())
                    .ja0202(dto.ja0202())
                    .ja0203(dto.ja0203())
                    .ja0204(dto.ja0204())
                    .ja0205(dto.ja0205())
                    .ja0301(dto.ja0301())
                    .ja0302(dto.ja0302())
                    .ja0303(dto.ja0303())
                    .ja0313(dto.ja0313())
                    .ja0314(dto.ja0314())
                    .ja0315(dto.ja0315())
                    .ja0316(dto.ja0316())
                    .ja0317(dto.ja0317())
                    .ja0318(dto.ja0318())
                    .ja0319(dto.ja0319())
                    .ja0320(dto.ja0320())
                    .ja0322(dto.ja0322())
                    .ja0326(dto.ja0326())
                    .ja0327(dto.ja0327())
                    .ja0401(dto.ja0401())
                    .ja0402(dto.ja0402())
                    .ja0403(dto.ja0403())
                    .ja0404(dto.ja0404())
                    .ja0410(dto.ja0410())
                    .ja0411(dto.ja0411())
                    .ja0412(dto.ja0412())
                    .ja0413(dto.ja0413())
                    .ja0414(dto.ja0414())
                    .ja1101(dto.ja1101())
                    .ja1102(dto.ja1102())
                    .ja1103(dto.ja1103())
                    .ja1201(dto.ja1201())
                    .ja1202(dto.ja1202())
                    .ja1299(dto.ja1299())
                    .ja2101(dto.ja2101())
                    .ja2102(dto.ja2102())
                    .ja2103(dto.ja2103())
                    .ja2201(dto.ja2201())
                    .ja2202(dto.ja2202())
                    .ja2203(dto.ja2203())
                    .ja2299(dto.ja2299())
                    .ja0328(dto.ja0328())
                    .ja0329(dto.ja0329())
                    .ja0330(dto.ja0330())
                    .build());
        }
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
