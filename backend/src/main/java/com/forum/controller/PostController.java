package com.forum.controller;

import com.forum.dto.*;
import com.forum.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public Result<PageResult<PostDTO>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return postService.getPosts(categoryId, page, size, userId);
    }

    @GetMapping("/{id}")
    public Result<PostDTO> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return postService.getPostDetail(id, userId);
    }

    @PostMapping
    public Result<PostDTO> create(@Valid @RequestBody PostRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return postService.createPost(req, userId);
    }

    @PostMapping("/{id}/like")
    public Result<PostDTO> toggleLike(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return postService.toggleLike(id, userId);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return postService.deletePost(id, userId);
    }

    @PostMapping("/{id}/favorite")
    public Result<PostDTO> toggleFavorite(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return postService.toggleFavorite(id, userId);
    }
}
