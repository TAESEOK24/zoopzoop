package com.zoopzoop.zoopzoop.domain.community.controller;

import com.zoopzoop.zoopzoop.domain.community.dto.*;
import com.zoopzoop.zoopzoop.domain.community.service.CommunityService;
import com.zoopzoop.zoopzoop.standard.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // 🚀 이 줄이 없어서 에리나 난 것입니다!

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

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentDto.Response>> getComments(@PathVariable Long postId) {
        return ApiResponse.ok(communityService.getComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<String> addComment(@PathVariable Long postId, @RequestBody CommentDto.Request request) {
        communityService.addComment(postId, request);
        return ApiResponse.ok("댓글 등록 완료");
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<String> deleteComment(@PathVariable Long commentId) {
        communityService.deleteComment(commentId);
        return ApiResponse.ok("댓글 삭제 완료");
    }
}