package com.zoopzoop.zoopzoop.domain.policy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyDetailDto(
        @JsonProperty("서비스ID") String serviceId,
        @JsonProperty("지원유형") String serviceType,
        @JsonProperty("서비스명") String serviceName,
        @JsonProperty("서비스목적") String purpose,
        @JsonProperty("지원대상") String target,
        @JsonProperty("선정기준") String selectionCriteria,
        @JsonProperty("지원내용") String supportContent,
        @JsonProperty("신청방법") String applicationMethod,
        @JsonProperty("구비서류") String requiredDocuments,
        @JsonProperty("접수기관명") String receivingOrgName,
        @JsonProperty("문의처") String contactInfo,
        @JsonProperty("온라인신청사이트URL") String onlineUrl,
        @JsonProperty("소관기관명") String orgName,
        @JsonProperty("부서명") String departmentName,
        @JsonProperty("행정규칙") String adminRule,
        @JsonProperty("자치법규") String localRule,
        @JsonProperty("법령") String law,
        @JsonProperty("공무원확인구비서류") String officialRequiredDocs,
        @JsonProperty("본인확인필요구비서류") String userRequiredDocs,
        @JsonProperty("수정일시") String updatedAt
) {}