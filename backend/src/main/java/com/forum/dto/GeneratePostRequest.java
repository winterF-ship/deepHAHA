package com.forum.dto;

import jakarta.validation.constraints.NotBlank;

public class GeneratePostRequest {

    @NotBlank(message = "话题不能为空")
    private String topic;

    private Long categoryId;

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
