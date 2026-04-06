package com.zoopzoop.zoopzoop.domain.policy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zoopzoop.zoopzoop.domain.policy.dto.PolicyDetailResultDto;
import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchResultDto;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyDetail;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyDetailRepository;
import com.zoopzoop.zoopzoop.domain.policy.repository.PolicyListRepository;
import com.zoopzoop.zoopzoop.global.exception.AppException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicySearchServiceTest {

    @Mock
    private PolicyListRepository policyListRepository;

    @Mock
    private PolicyDetailRepository policyDetailRepository;

    @InjectMocks
    private PolicySearchService policySearchService;

    @Test
    void searchPoliciesReturnsMappedResults() {
        PolicyList policy = PolicyList.builder()
                .serviceId("svc-1")
                .serviceName("청년 월세 지원")
                .purposeSummary("주거비 부담 완화")
                .target("청년")
                .supportContent("월세 일부 지원")
                .applicationMethod("온라인 신청")
                .applicationDeadline("상시")
                .detailUrl("https://example.com/policies/svc-1")
                .orgName("서울시")
                .departmentName("청년정책과")
                .viewCount(100)
                .createdAt(LocalDateTime.now())
                .build();

        when(policyListRepository.searchByKeyword(eq("청년"), any())).thenReturn(List.of(policy));

        List<PolicySearchResultDto> results = policySearchService.searchPolicies(" 청년 ", 20);

        assertEquals(1, results.size());
        assertEquals("svc-1", results.get(0).serviceId());
        assertEquals("청년 월세 지원", results.get(0).serviceName());
        verify(policyListRepository).searchByKeyword(eq("청년"), any());
    }

    @Test
    void searchPoliciesRejectsBlankKeyword() {
        AppException exception = assertThrows(AppException.class,
                () -> policySearchService.searchPolicies(" ", 5));

        assertEquals(400, exception.statusCode());
        assertEquals("검색어를 입력해주세요.", exception.getMessage());
    }

    @Test
    void getPolicyDetailReturnsListDataEvenWithoutDetailRow() {
        PolicyList policy = PolicyList.builder()
                .serviceId("svc-2")
                .serviceName("청년 교통비 지원")
                .purposeSummary("교통비 지원")
                .target("대학생")
                .selectionCriteria("소득 기준 충족")
                .supportContent("교통비 바우처 지급")
                .applicationMethod("방문 신청")
                .applicationDeadline("2026-12-31")
                .detailUrl("https://example.com/policies/svc-2")
                .orgName("부산시")
                .departmentName("복지정책과")
                .contactNumber("051-000-0000")
                .viewCount(10)
                .createdAt(LocalDateTime.now())
                .build();

        when(policyListRepository.findById("svc-2")).thenReturn(Optional.of(policy));
        when(policyDetailRepository.findById("svc-2")).thenReturn(Optional.empty());

        PolicyDetailResultDto result = policySearchService.getPolicyDetail("svc-2");

        assertEquals("청년 교통비 지원", result.serviceName());
        assertEquals("소득 기준 충족", result.selectionCriteria());
        assertNull(result.purpose());
    }

    @Test
    void getPolicyDetailIncludesDetailFields() {
        PolicyList policy = PolicyList.builder()
                .serviceId("svc-3")
                .serviceName("청년 문화 바우처")
                .purposeSummary("문화 활동 지원")
                .target("청년")
                .selectionCriteria("지역 거주")
                .supportContent("문화 바우처 지급")
                .applicationMethod("온라인 신청")
                .applicationDeadline("예산 소진 시")
                .detailUrl("https://example.com/policies/svc-3")
                .orgName("대구시")
                .departmentName("청년문화과")
                .contactNumber("053-000-0000")
                .viewCount(55)
                .createdAt(LocalDateTime.now())
                .build();
        PolicyDetail detail = PolicyDetail.builder()
                .serviceId("svc-3")
                .purpose("청년 문화 접근성 확대")
                .requiredDocuments("신분증")
                .receivingOrgName("대구시청")
                .contactInfo("053-111-1111")
                .onlineUrl("https://example.com/apply/svc-3")
                .adminRule("조례")
                .localRule("시행규칙")
                .law("청년기본법")
                .officialRequiredDocs("주민등록등본")
                .userRequiredDocs("학생증")
                .build();

        when(policyListRepository.findById("svc-3")).thenReturn(Optional.of(policy));
        when(policyDetailRepository.findById("svc-3")).thenReturn(Optional.of(detail));

        PolicyDetailResultDto result = policySearchService.getPolicyDetail("svc-3");

        assertEquals("청년 문화 접근성 확대", result.purpose());
        assertEquals("https://example.com/apply/svc-3", result.onlineUrl());
        assertEquals("청년기본법", result.law());
    }
}
