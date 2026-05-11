package com.zoopzoop.zoopzoop.domain.community.dto;

import lombok.*;

public class CommentDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long postId; // 🚀 [여기 추가됨!] 프론트엔드 이동을 위한 게시글 번호
        private String author;
        private String content;
        private String date;
    }
}