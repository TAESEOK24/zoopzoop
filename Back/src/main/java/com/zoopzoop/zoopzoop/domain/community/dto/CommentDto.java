package com.zoopzoop.zoopzoop.domain.community.dto;

import lombok.*;

public class CommentDto {
    // 프론트에서 글 쓸 때 받아올 내용
    @Getter @NoArgsConstructor
    public static class Request {
        private String content;
    }

    // 프론트로 보내줄 댓글 정보
    @Getter @Builder @AllArgsConstructor
    public static class Response {
        private Long id;
        private String author;
        private String content;
        private String date;
    }
}