package com.forum.controller;

import com.forum.dto.MessageReplyDTO;
import com.forum.dto.Result;
import com.forum.service.ReplyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MessageController {

    private final ReplyService replyService;

    public MessageController(ReplyService replyService) {
        this.replyService = replyService;
    }

    @GetMapping("/api/messages/replies")
    public Result<List<MessageReplyDTO>> getRepliesToMe(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(replyService.getRepliesToMe(userId));
    }
}
