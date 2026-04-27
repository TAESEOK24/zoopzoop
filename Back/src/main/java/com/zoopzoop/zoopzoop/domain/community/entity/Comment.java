package com.zoopzoop.zoopzoop.domain.community.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId; // 어느 게시글에 달린 댓글인지 기억하는 역할

    private String author;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String date;

    // 🚀 [추가됨] 댓글 내용을 수정할 때 사용할 스위치(메서드)
    public void updateContent(String content) {
        this.content = content;
    }
}