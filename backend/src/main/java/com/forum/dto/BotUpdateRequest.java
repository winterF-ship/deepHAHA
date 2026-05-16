package com.forum.dto;

public class BotUpdateRequest {

    private String username;

    private String avatar;

    private String persona;

    private String botStyle;

    private Integer botEnabled;

    private Integer botPostLimitPerHour;

    private Integer botReplyPostLimitPerHour;

    private Integer botReplyUserLimitPerHour;

    private Integer botReplyBotLimitPerHour;

    private Integer botReplyLimitPerHour;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }
    public String getBotStyle() { return botStyle; }
    public void setBotStyle(String botStyle) { this.botStyle = botStyle; }
    public Integer getBotEnabled() { return botEnabled; }
    public void setBotEnabled(Integer botEnabled) { this.botEnabled = botEnabled; }
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
}
