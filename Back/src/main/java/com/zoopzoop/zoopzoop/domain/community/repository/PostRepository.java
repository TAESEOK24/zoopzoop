package com.zoopzoop.zoopzoop.domain.community.repository;

import com.zoopzoop.zoopzoop.domain.community.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // 🚀 추가

public interface PostRepository extends JpaRepository<Post, Long> {
    // 검색어가 있을 때 페이징 처리
    Page<Post> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // 전체 목록 페이징 처리
    Page<Post> findAll(Pageable pageable);

    // 🚀 [추가됨] 작성자(이름)로 내가 쓴 글을 최신순으로 찾기
    List<Post> findByAuthorOrderByIdDesc(String author);
}