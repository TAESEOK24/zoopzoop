package com.zoopzoop.zoopzoop.domain.policy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyListDto(
        @JsonProperty("서비스ID") String serviceId,
        @JsonProperty("지원유형") String serviceType,
        @JsonProperty("서비스명") String serviceName,
        @JsonProperty("서비스목적요약") String purposeSummary,
        @JsonProperty("지원대상") String target,
        @JsonProperty("선정기준") String selectionCriteria,
        @JsonProperty("지원내용") String supportContent,
        @JsonProperty("신청방법") String applicationMethod,
        @JsonProperty("신청기한") String applicationDeadline,
        @JsonProperty("상세조회URL") String detailUrl,
        @JsonProperty("소관기관코드") String orgCode,
        @JsonProperty("소관기관명") String orgName,
        @JsonProperty("부서명") String departmentName,
        @JsonProperty("조회수") Integer viewCount,
        @JsonProperty("소관기관유형") String orgType,
        @JsonProperty("사용자구분") String userType,
        @JsonProperty("서비스분야") String serviceField,
        @JsonProperty("접수기관") String receivingOrg,
        @JsonProperty("전화문의") String contactNumber,
        @JsonProperty("등록일시") String createdAt,
        @JsonProperty("수정일시") String updatedAt
) {}