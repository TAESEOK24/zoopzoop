package com.zoopzoop.zoopzoop.domain.community.service;

import com.zoopzoop.zoopzoop.domain.community.dto.*;
import com.zoopzoop.zoopzoop.domain.community.entity.Post;
import com.zoopzoop.zoopzoop.domain.community.entity.Comment;
import com.zoopzoop.zoopzoop.domain.community.entity.Report;
import com.zoopzoop.zoopzoop.domain.community.repository.PostRepository;
import com.zoopzoop.zoopzoop.domain.community.repository.CommentRepository;
import com.zoopzoop.zoopzoop.domain.community.repository.ReportRepository;
import com.zoopzoop.zoopzoop.domain.user.entity.User;
import com.zoopzoop.zoopzoop.domain.user.repository.UserRepository;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
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
    private final ReportRepository reportRepository;

    // 🚀 무적의 로그인 유저 정보 파싱 로직
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        if (auth.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            return userRepository.findById(authenticatedUser.id())
                    .orElseThrow(() -> new IllegalArgumentException("?ъ슜?먮? 李얠쓣 ???놁뒿?덈떎."));
        }

        String principalStr = auth.getPrincipal().toString();
        String parsedEmail = principalStr;

        if (principalStr.contains("email=")) {
            int start = principalStr.indexOf("email=") + 6;
            int end = principalStr.indexOf(",", start);
            if (end == -1) end = principalStr.indexOf("]", start);
            if (end == -1) end = principalStr.indexOf(")", start);
            if (end == -1) end = principalStr.indexOf("'", start);
            if (end != -1) parsedEmail = principalStr.substring(start, end).trim();
        } else if (!parsedEmail.contains("@")) {
            parsedEmail = auth.getName();
        }

        return userRepository.findByEmail(parsedEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPosts(String category, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Post> postPage;

        boolean isAll = (category == null || category.trim().isEmpty() || category.equals("전체") || category.equals("전체글보기"));
        boolean isBest = "베스트 게시물 (HOT)".equals(category);
        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty());

        if (isAll) {
            postPage = hasKeyword ? postRepository.findByTitleContainingIgnoreCase(keyword, pageable) : postRepository.findAll(pageable);
        } else if (isBest) {
            postPage = hasKeyword ? postRepository.findByViewsGreaterThanEqualAndTitleContainingIgnoreCase(10L, keyword, pageable) : postRepository.findByViewsGreaterThanEqual(10L, pageable);
        } else {
            postPage = hasKeyword ? postRepository.findByCategoryAndTitleContainingIgnoreCase(category, keyword, pageable) : postRepository.findByCategory(category, pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("posts", postPage.getContent().stream()
                .map(post -> PostResponse.builder().id(post.getId()).type(post.getType()).category(post.getCategory())
                        .title(post.getTitle()).author(post.getAuthor()).date(post.getDate()).views(post.getViews()).build())
                .collect(Collectors.toList()));
        response.put("totalPages", postPage.getTotalPages());
        response.put("totalElements", postPage.getTotalElements());
        response.put("currentPage", postPage.getNumber());
        return response;
    }

    @Transactional
    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        post.incrementViews();
        return PostResponse.builder().id(post.getId()).type(post.getType()).category(post.getCategory()).title(post.getTitle())
                .content(post.getContent()).author(post.getAuthor()).date(post.getDate()).views(post.getViews()).build();
    }

    @Transactional
    public PostResponse createPost(PostCreateRequest request) {
        User user = getCurrentUser();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        Post post = Post.builder().type(request.getType() != null ? request.getType() : "일반").category(request.getCategory())
                .title(request.getTitle()).content(request.getContent()).author(user.getName()).date(today).views(0L).build();
        Post savedPost = postRepository.save(post);
        return PostResponse.builder().id(savedPost.getId()).build();
    }

    @Transactional
    public Long updatePost(Long id, PostUpdateRequest request) {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        User user = getCurrentUser();

        // 🚀 권한 체크
        if (!post.getAuthor().equals(user.getName()) && !user.getRole().name().equals("ADMIN")) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        post.update(request.getTitle(), request.getContent(), request.getCategory());
        return id;
    }

    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        User user = getCurrentUser();

        // 🚀 권한 체크
        if (!post.getAuthor().equals(user.getName()) && !user.getRole().name().equals("ADMIN")) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // 🚀 댓글 먼저 연쇄 삭제
        List<Comment> comments = commentRepository.findByPostId(id);
        if (!comments.isEmpty()) commentRepository.deleteAll(comments);

        postRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CommentDto.Response> getComments(Long postId) {
        return commentRepository.findByPostId(postId).stream()
                .map(comment -> CommentDto.Response.builder().id(comment.getId()).postId(comment.getPostId())
                        .author(comment.getAuthor()).content(comment.getContent()).date(comment.getDate()).build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void addComment(Long postId, CommentDto.Request request) {
        User user = getCurrentUser();
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
        Comment comment = Comment.builder().postId(postId).author(user.getName()).content(request.getContent()).date(today).build();
        commentRepository.save(comment);
    }

    @Transactional
    public void updateComment(Long commentId, CommentDto.Request request) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new IllegalArgumentException("댓글 없음"));
        User user = getCurrentUser();
        if (!comment.getAuthor().equals(user.getName()) && !user.getRole().name().equals("ADMIN")) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        comment.updateContent(request.getContent());
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new IllegalArgumentException("댓글 없음"));
        User user = getCurrentUser();
        if (!comment.getAuthor().equals(user.getName()) && !user.getRole().name().equals("ADMIN")) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        commentRepository.deleteById(commentId);
    }

    @Transactional
    public void createReport(ReportRequest request) {
        User user = getCurrentUser();
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
        Report report = Report.builder().targetType(request.getTargetType()).targetId(request.getTargetId())
                .reason(request.getReason()).reporter(user.getName()).status("PENDING").createdAt(today).build();
        reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts() {
        User user = getCurrentUser();
        return postRepository.findByAuthorOrderByIdDesc(user.getName()).stream()
                .map(post -> PostResponse.builder().id(post.getId()).type(post.getType()).category(post.getCategory())
                        .title(post.getTitle()).author(post.getAuthor()).date(post.getDate()).views(post.getViews()).build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentDto.Response> getMyComments() {
        User user = getCurrentUser();
        return commentRepository.findByAuthorOrderByIdDesc(user.getName()).stream()
                .map(comment -> CommentDto.Response.builder().id(comment.getId()).postId(comment.getPostId())
                        .author(comment.getAuthor()).content(comment.getContent()).date(comment.getDate()).build())
                .collect(Collectors.toList());
    }
}
