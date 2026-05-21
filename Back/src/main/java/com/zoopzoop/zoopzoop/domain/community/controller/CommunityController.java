package com.zoopzoop.zoopzoop.domain.community.controller;

import com.zoopzoop.zoopzoop.domain.community.dto.*;
import com.zoopzoop.zoopzoop.domain.community.service.CommunityService;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/posts")
    public ApiResponse<Map<String, Object>> getPosts(
            @RequestParam(required = false, defaultValue = "전체글보기") String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Map<String, Object> response = communityService.getPosts(category, keyword, page, size);
        return ApiResponse.ok(response);
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<PostResponse> getPost(@PathVariable Long id) {
        return ApiResponse.ok(communityService.getPost(id));
    }

    @PostMapping("/posts")
    public ApiResponse<PostResponse> createPost(@RequestBody PostCreateRequest request) {
        return ApiResponse.ok(communityService.createPost(request));
    }

    @PutMapping("/posts/{id}")
    public ApiResponse<Long> updatePost(@PathVariable Long id, @RequestBody PostUpdateRequest request) {
        return ApiResponse.ok(communityService.updatePost(id, request));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        communityService.deletePost(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentDto.Response>> getComments(@PathVariable Long postId) {
        return ApiResponse.ok(communityService.getComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<Void> addComment(@PathVariable Long postId, @RequestBody CommentDto.Request request) {
        communityService.addComment(postId, request);
        return ApiResponse.ok(null);
    }

    @PutMapping("/comments/{commentId}")
    public ApiResponse<Void> updateComment(@PathVariable Long commentId, @RequestBody CommentDto.Request request) {
        communityService.updateComment(commentId, request);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        communityService.deleteComment(commentId);
        return ApiResponse.ok(null);
    }

    // 🚨 신고 접수 API
    @PostMapping("/reports")
    public ApiResponse<String> createReport(@RequestBody ReportRequest request) {
        communityService.createReport(request);
        return ApiResponse.ok("신고가 접수되었습니다.");
    }

    // 🚀 나의 활동 (마이페이지) API
    @GetMapping("/my-posts")
    public ApiResponse<List<PostResponse>> getMyPosts() {
        return ApiResponse.ok(communityService.getMyPosts());
    }

    @GetMapping("/my-comments")
    public ApiResponse<List<CommentDto.Response>> getMyComments() {
        return ApiResponse.ok(communityService.getMyComments());
    }
}