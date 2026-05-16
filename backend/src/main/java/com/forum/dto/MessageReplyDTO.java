package com.forum.dto;

import java.time.LocalDateTime;

public class MessageReplyDTO {
    private Long replyId;
    private Long postId;
    private String postTitle;
    private Long fromUserId;
    private String fromUsername;
    private String fromAvatar;
    private Integer fromIsBot;
    private String replyType;
    private String content;
    private LocalDateTime createdAt;
    private Long parentReplyId;
    private Long rootReplyId;

    public Long getReplyId() { return replyId; }
    public void setReplyId(Long replyId) { this.replyId = replyId; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }
    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }
    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }
    public String getFromAvatar() { return fromAvatar; }
    public void setFromAvatar(String fromAvatar) { this.fromAvatar = fromAvatar; }
    public Integer getFromIsBot() { return fromIsBot; }
    public void setFromIsBot(Integer fromIsBot) { this.fromIsBot = fromIsBot; }
    public String getReplyType() { return replyType; }
    public void setReplyType(String replyType) { this.replyType = replyType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getParentReplyId() { return parentReplyId; }
    public void setParentReplyId(Long parentReplyId) { this.parentReplyId = parentReplyId; }
    public Long getRootReplyId() { return rootReplyId; }
    public void setRootReplyId(Long rootReplyId) { this.rootReplyId = rootReplyId; }
}
