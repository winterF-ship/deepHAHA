package com.forum.controller;

import com.forum.dto.Result;
import com.forum.entity.Notice;
import com.forum.repository.NoticeRepository;
import org.springframework.web.bind.annotation.*;

@RestController
public class NoticeController {

    private final NoticeRepository noticeRepository;

    public NoticeController(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @GetMapping("/api/notice")
    public Result<?> getNotice() {
        Notice notice = noticeRepository.findById(1L).orElse(null);
        if (notice == null || notice.getContent() == null) {
            return Result.success(null);
        }
        return Result.success(new NoticeDTO(notice));
    }

    public static class NoticeDTO {
        private String content;
        private String updateTime;
        private String updateBy;
        public NoticeDTO(Notice n) {
            this.content = n.getContent();
            this.updateTime = n.getUpdateTime() != null ? n.getUpdateTime().toString() : null;
            this.updateBy = n.getUpdateBy();
        }
        public String getContent() { return content; }
        public String getUpdateTime() { return updateTime; }
        public String getUpdateBy() { return updateBy; }
    }
}
