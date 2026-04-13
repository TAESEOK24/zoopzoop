package com.zoopzoop.zoopzoop.domain.community.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_type") // 예약어 충돌 방지
    private String type;

    private String category;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String author;

    @Column(name = "created_date") // 예약어 충돌 방지
    private String date;

    private Long views;

    // 🚀 이 부분이 없어서 첫 번째 에러가 났습니다! (조회수 증가 로직)
    public void incrementViews() {
        if (this.views == null) {
            this.views = 0L;
        }
        this.views += 1;
    }
    // 🚀 수정 기능 메서드 추가
    public void update(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }
}