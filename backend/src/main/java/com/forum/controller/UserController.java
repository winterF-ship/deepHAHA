package com.forum.controller;

import com.forum.dto.*;
import com.forum.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<UserDTO> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserDTO user = userService.getCurrentUser(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@Valid @RequestBody UpdateProfileRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.updateProfile(userId, req);
    }

    @PutMapping("/password")
    public Result<?> updatePassword(@Valid @RequestBody UpdatePasswordRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.updatePassword(userId, req);
    }

    @PostMapping("/avatar")
    public Result<?> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (file.isEmpty()) {
            return Result.error(400, "请选择文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只支持图片文件");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error(400, "图片大小不能超过2MB");
        }
        try {
            String ext = getExtension(file.getOriginalFilename());
            String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            Path uploadPath = Paths.get(System.getProperty("user.dir"), uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            File dest = uploadPath.resolve(filename).toFile();
            file.transferTo(dest);
            String avatarUrl = "/uploads/avatars/" + filename;
            return userService.updateAvatar(userId, avatarUrl);
        } catch (IOException e) {
            log.error("Avatar upload failed", e);
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "png";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
