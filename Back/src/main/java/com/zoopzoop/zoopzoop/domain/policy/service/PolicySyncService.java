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

    private static final String LIST_URL = "https://api.odcloud.kr/api/gov24/v3/serviceList";
    private static final String DETAIL_URL = "https://api.odcloud.kr/api/gov24/v3/serviceDetail";
    private static final String COND_URL = "https://api.odcloud.kr/api/gov24/v3/supportConditions";

    public String syncFullData() {
        int page = 1;
        int perPage = 100;
        int totalSaved = 0;

        log.info("Starting full policy sync");

        while (true) {
            try {
                log.info("Syncing page {}", page);

                String result = fetchAndSaveAll(page, perPage);
                if (result.startsWith("FAILED")) {
                    log.error("Stopping full sync at page {}: {}", page, result);
                    break;
                }

                totalSaved += perPage;
                page++;
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("Unexpected error during full policy sync", e);
                return "FAILED: sync aborted at page " + page;
            }

            if (page > 150) {
                break;
            }
        }

        log.info("Finished full policy sync. processed={}", totalSaved);
        return "SUCCESS: synced through page " + (page - 1);
    }

    @Transactional
    public String fetchAndSaveAll(int page, int perPage) {
        try {
            List<PolicyListDto> listDtos = fetchFromApi(LIST_URL, page, perPage, PolicyListDto.class);
            saveList(listDtos);

            List<PolicyDetailDto> detailDtos = fetchFromApi(DETAIL_URL, page, perPage, PolicyDetailDto.class);
            saveDetail(detailDtos);

            List<PolicyConditionsDto> conditionDtos = fetchFromApi(COND_URL, page, perPage, PolicyConditionsDto.class);
            saveConditions(conditionDtos);

            return "SUCCESS";
        } catch (Exception e) {
            log.error("Policy sync failed", e);
            return "FAILED: " + e.getMessage();
        }
    }

    private <T> List<T> fetchFromApi(String baseUrl, int page, int perPage, Class<T> clazz) throws Exception {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("page", page)
                .queryParam("perPage", perPage)
                .queryParam("serviceKey", serviceKey)
                .build(true)
                .toUri();

        String response = restTemplate.getForObject(uri, String.class);
        JsonNode dataNode = objectMapper.readTree(response).path("data");

        List<T> dtos = new ArrayList<>();
        if (dataNode.isArray()) {
            for (JsonNode node : dataNode) {
                dtos.add(objectMapper.treeToValue(node, clazz));
            }
        }
        return dtos;
    }

    private void saveList(List<PolicyListDto> dtos) {
        for (PolicyListDto dto : dtos) {
            if (dto.serviceId() == null || listRepo.existsById(dto.serviceId())) {
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
            if (dto.serviceId() == null || detailRepo.existsById(dto.serviceId())) {
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
            if (dto.serviceId() == null || conditionsRepo.existsById(dto.serviceId())) {
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
}
