package com.zoopzoop.zoopzoop.domain.community.repository;

import com.zoopzoop.zoopzoop.domain.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 특정 게시글(postId)의 댓글만 쏙 뽑아오는 마법의 주문!
    List<Comment> findByPostId(Long postId);
}