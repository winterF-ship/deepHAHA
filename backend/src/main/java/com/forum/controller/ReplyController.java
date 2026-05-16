package com.forum.controller;

import com.forum.dto.*;
import com.forum.service.ReplyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/replies")
public class ReplyController {

    private final ReplyService replyService;

    public ReplyController(ReplyService replyService) {
        this.replyService = replyService;
    }

    @PostMapping
    public Result<ReplyDTO> create(@PathVariable Long postId,
                                   @Valid @RequestBody ReplyRequest req,
                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return replyService.createReply(postId, req, userId);
    }

    @DeleteMapping("/{replyId}")
    public Result<?> delete(@PathVariable Long postId,
                            @PathVariable Long replyId,
                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return replyService.deleteReply(postId, replyId, userId);
    }
}
