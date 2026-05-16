package com.forum.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String authorDisplayTitle;
    private Integer authorMuted;
    private LocalDateTime authorMuteEndTime;
    private String authorMuteReason;
    private String authorRole;
    private Integer authorIsBot;
    private Long categoryId;
    private String categoryName;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReplyDTO> replies;
    private int replyCount;
    private int likeCount;
    private int favoriteCount;
    private boolean liked;
    private boolean favorited;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
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
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
    public Integer getAuthorIsBot() { return authorIsBot; }
    public void setAuthorIsBot(Integer authorIsBot) { this.authorIsBot = authorIsBot; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ReplyDTO> getReplies() { return replies; }
    public void setReplies(List<ReplyDTO> replies) { this.replies = replies; }
    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public boolean isFavorited() { return favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }
}
