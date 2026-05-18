package com.zoopzoop.zoopzoop.domain.community.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String targetType; // "POST" (게시글) 또는 "COMMENT" (댓글)
    private Long targetId;     // 글 번호 또는 댓글 번호
    private String reason;     // 신고 사유
    private String reporter;   // 신고자

    private String status;     // "PENDING"(대기중), "RESOLVED"(처리완료)
    private String createdAt;  // 신고 날짜

    public void resolve() {
        this.status = "RESOLVED";
    }
}