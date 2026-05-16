package com.forum.dto;

import java.time.LocalDateTime;

public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String avatar;
    private String role;
    private String displayTitle;
    private Integer muted;
    private LocalDateTime muteEndTime;
    private String muteReason;
    private Integer isBot;
    private Integer botEnabled;
    private String persona;
    private String botStyle;
    private Integer botPostLimitPerHour;
    private Integer botReplyPostLimitPerHour;
    private Integer botReplyUserLimitPerHour;
    private Integer botReplyBotLimitPerHour;
    private Integer botReplyLimitPerHour;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDisplayTitle() { return displayTitle; }
    public void setDisplayTitle(String displayTitle) { this.displayTitle = displayTitle; }
    public Integer getMuted() { return muted; }
    public void setMuted(Integer muted) { this.muted = muted; }
    public LocalDateTime getMuteEndTime() { return muteEndTime; }
    public void setMuteEndTime(LocalDateTime muteEndTime) { this.muteEndTime = muteEndTime; }
    public String getMuteReason() { return muteReason; }
    public void setMuteReason(String muteReason) { this.muteReason = muteReason; }
    public Integer getIsBot() { return isBot; }
    public void setIsBot(Integer isBot) { this.isBot = isBot; }
    public Integer getBotEnabled() { return botEnabled; }
    public void setBotEnabled(Integer botEnabled) { this.botEnabled = botEnabled; }
    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }
    public String getBotStyle() { return botStyle; }
    public void setBotStyle(String botStyle) { this.botStyle = botStyle; }
    public Integer getBotPostLimitPerHour() { return botPostLimitPerHour; }
    public void setBotPostLimitPerHour(Integer botPostLimitPerHour) { this.botPostLimitPerHour = botPostLimitPerHour; }
    public Integer getBotReplyPostLimitPerHour() { return botReplyPostLimitPerHour; }
    public void setBotReplyPostLimitPerHour(Integer botReplyPostLimitPerHour) { this.botReplyPostLimitPerHour = botReplyPostLimitPerHour; }
    public Integer getBotReplyUserLimitPerHour() { return botReplyUserLimitPerHour; }
    public void setBotReplyUserLimitPerHour(Integer botReplyUserLimitPerHour) { this.botReplyUserLimitPerHour = botReplyUserLimitPerHour; }
    public Integer getBotReplyBotLimitPerHour() { return botReplyBotLimitPerHour; }
    public void setBotReplyBotLimitPerHour(Integer botReplyBotLimitPerHour) { this.botReplyBotLimitPerHour = botReplyBotLimitPerHour; }
    public Integer getBotReplyLimitPerHour() { return botReplyLimitPerHour; }
    public void setBotReplyLimitPerHour(Integer botReplyLimitPerHour) { this.botReplyLimitPerHour = botReplyLimitPerHour; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
