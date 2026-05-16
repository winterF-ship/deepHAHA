package com.forum.controller;

import com.forum.dto.Result;
import com.forum.dto.UserDTO;
import com.forum.entity.Notice;
import com.forum.repository.NoticeRepository;
import com.forum.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final NoticeRepository noticeRepository;

    public AdminController(UserService userService, NoticeRepository noticeRepository) {
        this.userService = userService;
        this.noticeRepository = noticeRepository;
    }

    @PutMapping("/notice")
    public Result<?> updateNotice(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var currentUser = userService.getCurrentUser(userId);
        if (currentUser == null || (!"ADMIN".equals(currentUser.getRole()) && !"SUPERVISOR".equals(currentUser.getRole()))) {
            return Result.error(403, "没有权限执行此操作");
        }
        Notice notice = noticeRepository.findById(1L).orElse(new Notice());
        notice.setContent(body.get("content"));
        notice.setUpdateTime(LocalDateTime.now());
        notice.setUpdateBy(currentUser.getUsername());
        noticeRepository.save(notice);
        return Result.success("公告更新成功");
    }

    @GetMapping("/users")
    public Result<List<UserDTO>> listUsers(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.getAllUsers(userId);
    }

    @PutMapping("/users/{userId}/mute")
    public Result<?> muteUser(@PathVariable Long userId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        Long days = body.get("days") != null ? ((Number) body.get("days")).longValue() : null;
        String reason = (String) body.get("reason");
        return userService.muteUser(userId, days, reason, operatorId);
    }

    @PutMapping("/users/{userId}/unmute")
    public Result<?> unmuteUser(@PathVariable Long userId, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        return userService.unmuteUser(userId, operatorId);
    }

    @PostMapping("/users/{userId}/supervisor")
    public Result<?> appointSupervisor(@PathVariable Long userId, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        return userService.appointSupervisor(userId, operatorId);
    }

    @DeleteMapping("/users/{userId}/supervisor")
    public Result<?> removeSupervisor(@PathVariable Long userId, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        return userService.removeSupervisor(userId, operatorId);
    }

    @PutMapping("/users/{userId}/display-title")
    public Result<?> updateDisplayTitle(@PathVariable Long userId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        return userService.updateDisplayTitle(userId, body.get("displayTitle"), operatorId);
    }

    @DeleteMapping("/users/{userId}")
    public Result<?> deleteUser(@PathVariable Long userId, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        return userService.deleteUser(userId, operatorId);
    }
}
