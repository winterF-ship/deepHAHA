package com.forum.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_notice")
public class Notice {

    @Id
    private Long id = 1L;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "update_by", length = 50)
    private String updateBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
