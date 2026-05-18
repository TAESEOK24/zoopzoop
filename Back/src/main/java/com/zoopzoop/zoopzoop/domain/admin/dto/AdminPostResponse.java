package com.zoopzoop.zoopzoop.domain.admin.dto;

import com.zoopzoop.zoopzoop.domain.community.entity.Post;

public record AdminPostResponse(
        Long id,
        String title,
        String authorName
) {
    public static AdminPostResponse from(Post post) {
        return new AdminPostResponse(
                post.getId(),
                post.getTitle(),
                post.getAuthor()
        );
    }
}