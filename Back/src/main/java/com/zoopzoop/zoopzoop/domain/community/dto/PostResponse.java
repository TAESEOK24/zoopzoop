package com.zoopzoop.zoopzoop.domain.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String type;
    private String category;
    private String title;

    // 🚀 이 줄이 없어서 두 번째 에러가 났습니다! (본문 데이터를 담을 곳)
    private String content;

    private String author;
    private String date;
    private Long views;
}