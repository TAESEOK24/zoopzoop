package com.zoopzoop.zoopzoop.domain.community.repository;

import com.zoopzoop.zoopzoop.domain.community.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // 🚀 1. 카테고리로 필터링 (공지사항, 자유게시판 등)
    Page<Post> findByCategory(String category, Pageable pageable);
    Page<Post> findByCategoryAndTitleContainingIgnoreCase(String category, String title, Pageable pageable);

    // 🚀 2. 베스트 게시물 (조회수 10 이상)
    Page<Post> findByViewsGreaterThanEqual(Long views, Pageable pageable);
    Page<Post> findByViewsGreaterThanEqualAndTitleContainingIgnoreCase(Long views, String title, Pageable pageable);

    List<Post> findByAuthorOrderByIdDesc(String author);
}