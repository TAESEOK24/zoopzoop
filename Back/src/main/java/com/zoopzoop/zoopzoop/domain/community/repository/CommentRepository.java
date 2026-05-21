package com.zoopzoop.zoopzoop.domain.community.repository;

import com.zoopzoop.zoopzoop.domain.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId); // 🚀 이 한 줄이 없어서 글 삭제가 안 됐습니다!
    List<Comment> findByAuthorOrderByIdDesc(String author);
}