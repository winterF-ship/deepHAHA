package com.forum.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ReplyDTO {
    private Long id;
    private String content;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String authorDisplayTitle;
    private Integer authorMuted;
    private LocalDateTime authorMuteEndTime;
    private String authorMuteReason;
    private Integer floorNo;
    private Long parentReplyId;
    private String parentAuthorName;
    private Long replyToUserId;
    private String replyToUsername;
    private String authorRole;
    private Integer authorIsBot;
    private Integer deleted;
    private LocalDateTime createdAt;
    private Long rootReplyId;
    private String replyType;
    private List<ReplyDTO> subReplies;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public String getAuthorDisplayTitle() { return authorDisplayTitle; }
    public void setAuthorDisplayTitle(String authorDisplayTitle) { this.authorDisplayTitle = authorDisplayTitle; }
    public Integer getAuthorMuted() { return authorMuted; }
    public void setAuthorMuted(Integer authorMuted) { this.authorMuted = authorMuted; }
    public LocalDateTime getAuthorMuteEndTime() { return authorMuteEndTime; }
    public void setAuthorMuteEndTime(LocalDateTime authorMuteEndTime) { this.authorMuteEndTime = authorMuteEndTime; }
    public String getAuthorMuteReason() { return authorMuteReason; }
    public void setAuthorMuteReason(String authorMuteReason) { this.authorMuteReason = authorMuteReason; }
    public Integer getFloorNo() { return floorNo; }
    public void setFloorNo(Integer floorNo) { this.floorNo = floorNo; }
    public Long getParentReplyId() { return parentReplyId; }
    public void setParentReplyId(Long parentReplyId) { this.parentReplyId = parentReplyId; }
    public String getParentAuthorName() { return parentAuthorName; }
    public void setParentAuthorName(String parentAuthorName) { this.parentAuthorName = parentAuthorName; }
    public Long getReplyToUserId() { return replyToUserId; }
    public void setReplyToUserId(Long replyToUserId) { this.replyToUserId = replyToUserId; }
    public String getReplyToUsername() { return replyToUsername; }
    public void setReplyToUsername(String replyToUsername) { this.replyToUsername = replyToUsername; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
    public Integer getAuthorIsBot() { return authorIsBot; }
    public void setAuthorIsBot(Integer authorIsBot) { this.authorIsBot = authorIsBot; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getRootReplyId() { return rootReplyId; }
    public void setRootReplyId(Long rootReplyId) { this.rootReplyId = rootReplyId; }
    public String getReplyType() { return replyType; }
    public void setReplyType(String replyType) { this.replyType = replyType; }
    public List<ReplyDTO> getSubReplies() { return subReplies; }
    public void setSubReplies(List<ReplyDTO> subReplies) { this.subReplies = subReplies; }
}
