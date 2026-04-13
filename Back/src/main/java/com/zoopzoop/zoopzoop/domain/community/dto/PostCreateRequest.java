package com.zoopzoop.zoopzoop.domain.community.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequest {
    private String type;
    private String category;
    private String title;
    private String content;
    private String author; // 임시로 프론트에서 받음 (나중엔 로그인 토큰에서 추출)
}