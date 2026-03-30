package com.zoopzoop.zoopzoop.domain.policy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyConditionsDto(
        @JsonProperty("서비스ID") String serviceId,
        @JsonProperty("서비스명") String serviceName,

        // 연령 관련 (Integer)
        @JsonProperty("JA0110") Integer ja0110, // 시작연령
        @JsonProperty("JA0111") Integer ja0111, // 종료연령

        // 조건 여부 (String - Y, N, null 등으로 들어옵니다)
        @JsonProperty("JA0101") String ja0101, // 남성
        @JsonProperty("JA0102") String ja0102, // 여성
        @JsonProperty("JA0201") String ja0201, // 중위소득 0~50%
        @JsonProperty("JA0202") String ja0202, // 중위소득 51~75%
        @JsonProperty("JA0203") String ja0203, // 중위소득 76~100%
        @JsonProperty("JA0204") String ja0204, // 중위소득 101~200%
        @JsonProperty("JA0205") String ja0205, // 중위소득 200% 초과
        @JsonProperty("JA0301") String ja0301, // 예비부모/난임
        @JsonProperty("JA0302") String ja0302, // 임산부
        @JsonProperty("JA0303") String ja0303, // 출산/입양
        @JsonProperty("JA0313") String ja0313, // 농업인
        @JsonProperty("JA0314") String ja0314, // 어업인
        @JsonProperty("JA0315") String ja0315, // 축산업인
        @JsonProperty("JA0316") String ja0316, // 임업인
        @JsonProperty("JA0317") String ja0317, // 초등학생
        @JsonProperty("JA0318") String ja0318, // 중학생
        @JsonProperty("JA0319") String ja0319, // 고등학생
        @JsonProperty("JA0320") String ja0320, // 대학생/대학원생
        @JsonProperty("JA0322") String ja0322, // 해당사항없음(학력)
        @JsonProperty("JA0326") String ja0326, // 근로자/직장인
        @JsonProperty("JA0327") String ja0327, // 구직자/실업자
        @JsonProperty("JA0401") String ja0401, // 다문화가족
        @JsonProperty("JA0402") String ja0402, // 북한이탈주민
        @JsonProperty("JA0403") String ja0403, // 한부모가족/조손가족
        @JsonProperty("JA0404") String ja0404, // 1인가구
        @JsonProperty("JA0410") String ja0410, // 해당사항없음(가구)
        @JsonProperty("JA0411") String ja0411, // 다자녀가구
        @JsonProperty("JA0412") String ja0412, // 무주택세대
        @JsonProperty("JA0413") String ja0413, // 신규전입
        @JsonProperty("JA0414") String ja0414, // 확대가족
        @JsonProperty("JA1101") String ja1101, // 예비창업자
        @JsonProperty("JA1102") String ja1102, // 영업중
        @JsonProperty("JA1103") String ja1103, // 생계곤란/폐업예정자
        @JsonProperty("JA1201") String ja1201, // 음식점업
        @JsonProperty("JA1202") String ja1202, // 제조업(소상공인)
        @JsonProperty("JA1299") String ja1299, // 기타업종(소상공인)
        @JsonProperty("JA2101") String ja2101, // 중소기업
        @JsonProperty("JA2102") String ja2102, // 사회복지시설
        @JsonProperty("JA2103") String ja2103, // 기관/단체
        @JsonProperty("JA2201") String ja2201, // 제조업(기업)
        @JsonProperty("JA2202") String ja2202, // 농어업
        @JsonProperty("JA2203") String ja2203, // 정보통신업
        @JsonProperty("JA2299") String ja2299, // 기타업종(기업)
        @JsonProperty("JA0328") String ja0328, // 장애인
        @JsonProperty("JA0329") String ja0329, // 국가보훈대상자
        @JsonProperty("JA0330") String ja0330  // 질병/질환자
) {}