package com.zoopzoop.zoopzoop.domain.community.dto;
import lombok.*;

public class CommentDto {
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {
        private String content;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private Long postId;
        private String author;
        private String content;
        private String date;
    }
}