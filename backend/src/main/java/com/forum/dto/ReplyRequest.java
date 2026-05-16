package com.forum.dto;

import jakarta.validation.constraints.NotBlank;

public class ReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    private String content;

    private Long parentReplyId;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getParentReplyId() { return parentReplyId; }
    public void setParentReplyId(Long parentReplyId) { this.parentReplyId = parentReplyId; }
}
