package com.zoopzoop.zoopzoop.domain.community.service;

import com.zoopzoop.zoopzoop.domain.community.dto.*;
import com.zoopzoop.zoopzoop.domain.community.entity.Post;
import com.zoopzoop.zoopzoop.domain.community.entity.Comment;
import com.zoopzoop.zoopzoop.domain.community.repository.PostRepository;
import com.zoopzoop.zoopzoop.domain.community.repository.CommentRepository;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /**
     * 1. 게시글 목록 조회 (검색어 + 페이징 포함)
     */
    public Map<String, Object> getPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Post> postPage;
        if (keyword != null && !keyword.isEmpty()) {
            postPage = postRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        } else {
            postPage = postRepository.findAll(pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("posts", postPage.getContent().stream()
                .map(post -> PostResponse.builder()
                        .id(post.getId())
                        .type(post.getType())
                        .category(post.getCategory())
                        .title(post.getTitle())
                        .author(post.getAuthor())
                        .date(post.getDate())
                        .views(post.getViews())
                        .build())
                .collect(Collectors.toList()));

        response.put("totalPages", postPage.getTotalPages());
        response.put("totalElements", postPage.getTotalElements());
        response.put("currentPage", postPage.getNumber());

        return response;
    }

    /**
     * 2. 게시글 상세 조회 (조회수 증가 포함)
     */
    @Transactional
    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. id=" + id));

        post.incrementViews();

        return PostResponse.builder()
                .id(post.getId())
                .type(post.getType())
                .category(post.getCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .author(post.getAuthor())
                .date(post.getDate())
                .views(post.getViews())
                .build();
    }

    /**
     * 3. 새 게시글 작성
     */
    @Transactional
    public PostResponse createPost(PostCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentInfo = authentication.getName();

        String parsedEmail = currentInfo;
        if (currentInfo.contains("email=")) {
            int start = currentInfo.indexOf("email=") + 6;
            int end = currentInfo.indexOf(",", start);
            if (end == -1) end = currentInfo.indexOf("]", start);
            if (end != -1) parsedEmail = currentInfo.substring(start, end).trim();
        }

        // 🚀 람다식 에러 방지를 위해 변하지 않는 final 변수에 담기!
        final String finalEmail = parsedEmail;

        if (finalEmail == null || finalEmail.isEmpty() || finalEmail.equals("anonymousUser")) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        User user = userRepository.findByEmail(finalEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 찾으려던 이메일: " + finalEmail));

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        Post post = Post.builder()
                .type(request.getType() != null ? request.getType() : "일반")
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .author(user.getName())
                .date(today)
                .views(0L)
                .build();

        Post savedPost = postRepository.save(post);

        return PostResponse.builder()
                .id(savedPost.getId())
                .build();
    }

    /**
     * 4. 게시글 수정
     */
    @Transactional
    public Long updatePost(Long id, PostUpdateRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));

        post.update(request.getTitle(), request.getContent(), request.getCategory());

        return id;
    }

    /**
     * 5. 게시글 삭제
     */
    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));

        postRepository.delete(post);
    }

    // ================= 댓글 기능 (Comments) ================= //

    /**
     * 6. 특정 게시글의 댓글 목록 조회
     */
    public List<CommentDto.Response> getComments(Long postId) {
        return commentRepository.findByPostId(postId).stream()
                .map(comment -> CommentDto.Response.builder()
                        .id(comment.getId())
                        .author(comment.getAuthor())
                        .content(comment.getContent())
                        .date(comment.getDate())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 7. 댓글 작성
     */
    @Transactional
    public void addComment(Long postId, CommentDto.Request request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentInfo = authentication.getName();

        String parsedEmail = currentInfo;
        if (currentInfo.contains("email=")) {
            int start = currentInfo.indexOf("email=") + 6;
            int end = currentInfo.indexOf(",", start);
            if (end == -1) end = currentInfo.indexOf("]", start);
            if (end != -1) parsedEmail = currentInfo.substring(start, end).trim();
        }

        // 🚀 람다식 에러 방지를 위해 변하지 않는 final 변수에 담기!
        final String finalEmail = parsedEmail;

        User user = userRepository.findByEmail(finalEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 찾으려던 이메일: " + finalEmail));

        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));

        Comment comment = Comment.builder()
                .postId(postId)
                .author(user.getName())
                .content(request.getContent())
                .date(today)
                .build();

        commentRepository.save(comment);
    }

    /**
     * 8. 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new IllegalArgumentException("해당 댓글이 존재하지 않습니다. id=" + commentId);
        }
        commentRepository.deleteById(commentId);
    }
}