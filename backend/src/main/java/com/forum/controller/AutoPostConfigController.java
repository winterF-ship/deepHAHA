package com.forum.controller;

import com.forum.config.DeepSeekConfig;
import com.forum.dto.Result;
import com.forum.entity.User;
import com.forum.repository.UserRepository;
import com.forum.service.AiAutoPostService;
import com.forum.service.BotReplyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AutoPostConfigController {

    private final AiAutoPostService aiAutoPostService;
    private final BotReplyService botReplyService;
    private final DeepSeekConfig deepSeekConfig;
    private final UserRepository userRepository;

    public AutoPostConfigController(AiAutoPostService aiAutoPostService,
                                    BotReplyService botReplyService,
                                    DeepSeekConfig deepSeekConfig,
                                    UserRepository userRepository) {
        this.aiAutoPostService = aiAutoPostService;
        this.botReplyService = botReplyService;
        this.deepSeekConfig = deepSeekConfig;
        this.userRepository = userRepository;
    }

    @GetMapping("/system-status")
    public Result<Map<String, Object>> getSystemStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!isAdmin(userId)) {
            return Result.error(403, "只有管理员才能查看系统状态");
        }
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("autoPostEnabled", aiAutoPostService.isEnabled());
        status.put("autoReplyEnabled", botReplyService.isAutoReplyEnabled());
        status.put("deepseekApiKeySet", deepSeekConfig.getKey() != null && !deepSeekConfig.getKey().isBlank());
        return Result.success(status);
    }

    @GetMapping("/auto-post-config")
    public Result<Map<String, Object>> getConfig(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!isAdmin(userId)) {
            return Result.error(403, "只有管理员才能查看AI自动发帖配置");
        }
        return Result.success(aiAutoPostService.getConfigSnapshot());
    }

    @PutMapping("/auto-post-config")
    public Result<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> body,
                                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!isAdmin(userId)) {
            return Result.error(403, "只有管理员才能修改AI自动发帖配置");
        }

        if (body.containsKey("enabled")) {
            aiAutoPostService.setRuntimeEnabled(toBoolean(body.get("enabled")));
        }
        if (body.containsKey("categoryIds")) {
            aiAutoPostService.setRuntimeCategoryIds(toString(body.get("categoryIds")));
        }
        if (body.containsKey("userIds")) {
            aiAutoPostService.setRuntimeUserIds(toString(body.get("userIds")));
        }
        if (body.containsKey("topicStyle")) {
            aiAutoPostService.setRuntimeTopicStyle(toString(body.get("topicStyle")));
        }
        if (body.containsKey("minTitleLength")) {
            aiAutoPostService.setRuntimeMinTitleLength(toInteger(body.get("minTitleLength")));
        }
        if (body.containsKey("maxTitleLength")) {
            aiAutoPostService.setRuntimeMaxTitleLength(toInteger(body.get("maxTitleLength")));
        }
        if (body.containsKey("minContentLength")) {
            aiAutoPostService.setRuntimeMinContentLength(toInteger(body.get("minContentLength")));
        }
        if (body.containsKey("maxContentLength")) {
            aiAutoPostService.setRuntimeMaxContentLength(toInteger(body.get("maxContentLength")));
        }

        Map<String, Object> snapshot = aiAutoPostService.getConfigSnapshot();
        snapshot.put("note", "除 intervalMs 需重启外，其余配置在下次定时任务执行时生效。运行时修改的配置重启后丢失，持久化请修改 application.properties。");
        return Result.success("配置已更新", snapshot);
    }

    private Boolean toBoolean(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return null;
    }

    private String toString(Object v) {
        return v == null ? null : v.toString();
    }

    private Integer toInteger(Object v) {
        if (v instanceof Integer i) return i;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        User u = userRepository.findById(userId).orElse(null);
        return u != null && "ADMIN".equals(u.getRole());
    }
}
