package com.forum.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    @JsonIgnore
    private String password;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String avatar;

    @Column(nullable = false, length = 10)
    private String role = "USER";

    @Column(name = "display_title", length = 50)
    private String displayTitle;

    @Column(nullable = false)
    private Integer muted = 0;

    @Column(name = "mute_end_time")
    private LocalDateTime muteEndTime;

    @Column(name = "mute_reason", length = 255)
    private String muteReason;

    @Column(nullable = false)
    private Integer isBot = 0;

    @Column(nullable = false)
    private Integer botEnabled = 0;

    @Column(length = 500)
    private String persona;

    @Column(name = "bot_style", length = 100)
    private String botStyle;

    @Column(name = "bot_post_limit_per_hour")
    private Integer botPostLimitPerHour = 3;

    @Column(name = "bot_reply_post_limit_per_hour")
    private Integer botReplyPostLimitPerHour = 5;

    @Column(name = "bot_reply_user_limit_per_hour")
    private Integer botReplyUserLimitPerHour = 5;

    @Column(name = "bot_reply_bot_limit_per_hour")
    private Integer botReplyBotLimitPerHour = 3;

    @Column(name = "bot_reply_limit_per_hour")
    private Integer botReplyLimitPerHour = 3;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public User() {}

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
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
