package com.zoopzoop.zoopzoop.domain.community.controller;

import com.zoopzoop.zoopzoop.domain.community.dto.*;
import com.zoopzoop.zoopzoop.domain.community.service.CommunityService;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
// 🚀 [추가됨] 인증 관련 임포트
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.zoopzoop.zoopzoop.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    // 1. 게시글 목록 조회 (검색어 + 페이징)
    @GetMapping("/posts")
    public ApiResponse<Map<String, Object>> getPosts(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.ok(communityService.getPosts(search, page, size));
    }

    // 2. 게시글 상세 조회
    @GetMapping("/posts/{id}")
    public ApiResponse<PostResponse> getPost(@PathVariable Long id) {
        return ApiResponse.ok(communityService.getPost(id));
    }

    // 3. 새 게시글 작성
    @PostMapping("/posts")
    public ApiResponse<PostResponse> createPost(@RequestBody PostCreateRequest request) {
        return ApiResponse.ok(communityService.createPost(request));
    }

    // 4. 게시글 수정
    @PutMapping("/posts/{id}")
    public ApiResponse<Long> updatePost(@PathVariable Long id, @RequestBody PostUpdateRequest request) {
        return ApiResponse.ok(communityService.updatePost(id, request));
    }

    // 5. 게시글 삭제
    @DeleteMapping("/posts/{id}")
    public ApiResponse<String> deletePost(@PathVariable Long id) {
        communityService.deletePost(id);
        return ApiResponse.ok("삭제 완료");
    }

    // ================= 댓글 API ================= //

    // 댓글 목록 조회
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentDto.Response>> getComments(@PathVariable Long postId) {
        return ApiResponse.ok(communityService.getComments(postId));
    }

    // 댓글 작성
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<String> addComment(@PathVariable Long postId, @RequestBody CommentDto.Request request) {
        communityService.addComment(postId, request);
        return ApiResponse.ok("댓글 등록 완료");
    }

    // 댓글 수정
    @PutMapping("/comments/{commentId}")
    public ApiResponse<String> updateComment(@PathVariable Long commentId, @RequestBody CommentDto.Request request) {
        communityService.updateComment(commentId, request);
        return ApiResponse.ok("댓글 수정 완료");
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<String> deleteComment(@PathVariable Long commentId) {
        communityService.deleteComment(commentId);
        return ApiResponse.ok("댓글 삭제 완료");
    }

    // ================= 🚀 [추가됨] 마이페이지 활동내역 API ================= //

    // 내가 쓴 게시글 조회 API
    @GetMapping("/my-posts")
    public ApiResponse<List<PostResponse>> getMyPosts(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(communityService.getMyPosts(currentUser.id()));
    }

    // 내가 쓴 댓글 조회 API
    @GetMapping("/my-comments")
    public ApiResponse<List<CommentDto.Response>> getMyComments(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(communityService.getMyComments(currentUser.id()));
    }
}