package com.forum.service;

import com.forum.dto.*;
import com.forum.entity.User;
import com.forum.repository.PostRepository;
import com.forum.repository.ReplyRepository;
import com.forum.repository.UserRepository;
import com.forum.util.JwtUtil;
import com.forum.util.RoleConstants;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReplyRepository replyRepository;
    private final JwtUtil jwtUtil;
    private final CaptchaStore captchaStore;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, PostRepository postRepository,
                       ReplyRepository replyRepository, JwtUtil jwtUtil, CaptchaStore captchaStore) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.jwtUtil = jwtUtil;
        this.captchaStore = captchaStore;
    }

    public Result<UserDTO> register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            return Result.error(400, "用户名已存在");
        }
        User user = new User(req.getUsername(), encoder.encode(req.getPassword()), req.getEmail());
        user = userRepository.save(user);
        return Result.success("注册成功", toDTO(user));
    }

    public Result<?> login(LoginRequest req) {
        // captcha validation
        if (req.getCaptchaId() == null || req.getCaptchaId().isBlank()) {
            return Result.error(400, "验证码已过期");
        }
        if (req.getCaptchaCode() == null || req.getCaptchaCode().isBlank()) {
            return Result.error(400, "请输入验证码");
        }
        String storedCode = captchaStore.getAndRemove(req.getCaptchaId());
        if (storedCode == null) {
            return Result.error(400, "验证码已过期");
        }
        if (!storedCode.equalsIgnoreCase(req.getCaptchaCode())) {
            return Result.error(400, "验证码错误");
        }

        User user = userRepository.findByUsername(req.getUsername()).orElse(null);
        if (user == null || !encoder.matches(req.getPassword(), user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        if (user.getIsBot() != null && user.getIsBot() == 1) {
            return Result.error(403, "AI角色不能登录");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return Result.success("登录成功", new LoginResponse(token, toDTO(user)));
    }

    public UserDTO getCurrentUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null ? toDTO(user) : null;
    }

    public Result<?> updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return Result.error(404, "用户不存在");
        if (!user.getUsername().equals(req.getUsername()) && userRepository.existsByUsername(req.getUsername())) {
            return Result.error(400, "用户名已被占用");
        }
        user.setUsername(req.getUsername());
        userRepository.save(user);
        return Result.success("修改成功", toDTO(user));
    }

    public Result<?> updatePassword(Long userId, UpdatePasswordRequest req) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return Result.error(404, "用户不存在");
        if (!encoder.matches(req.getOldPassword(), user.getPassword())) {
            return Result.error(400, "旧密码不正确");
        }
        user.setPassword(encoder.encode(req.getNewPassword()));
        userRepository.save(user);
        return Result.success("密码修改成功");
    }

    public Result<?> updateAvatar(Long userId, String avatar) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return Result.error(404, "用户不存在");
        user.setAvatar(avatar);
        userRepository.save(user);
        return Result.success("头像更新成功", toDTO(user));
    }

    public Result<?> muteUser(Long targetUserId, Long days, String reason, Long operatorId) {
        User operator = userRepository.findById(operatorId).orElse(null);
        if (operator == null || !"ADMIN".equals(operator.getRole())) {
            return Result.error(403, "没有权限执行此操作");
        }
        User target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) return Result.error(404, "用户不存在");
        if (targetUserId.equals(operatorId)) return Result.error(400, "不能禁言自己");
        if ("ADMIN".equals(target.getRole())) return Result.error(400, "不能禁言其他管理员");
        target.setMuted(1);
        target.setMuteEndTime(days == null ? null : LocalDateTime.now().plusDays(days));
        target.setMuteReason(reason);
        userRepository.save(target);
        return Result.success("禁言成功");
    }

    public Result<?> unmuteUser(Long targetUserId, Long operatorId) {
        User operator = userRepository.findById(operatorId).orElse(null);
        if (operator == null || !"ADMIN".equals(operator.getRole())) {
            return Result.error(403, "没有权限执行此操作");
        }
        User target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) return Result.error(404, "用户不存在");
        if (targetUserId.equals(operatorId)) return Result.error(400, "不能解除自己的禁言");
        if ("ADMIN".equals(target.getRole())) return Result.error(400, "不能对管理员执行解禁操作");
        target.setMuted(0);
        target.setMuteEndTime(null);
        target.setMuteReason(null);
        userRepository.save(target);
        return Result.success("已解除禁言");
    }

    public Result<List<UserDTO>> getAllUsers(Long operatorId) {
        User operator = userRepository.findById(operatorId).orElse(null);
        if (operator == null || !"ADMIN".equals(operator.getRole())) {
            return Result.error(403, "没有权限查看用户列表");
        }
        List<UserDTO> users = userRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
        return Result.success(users);
    }

    public Result<?> appointSupervisor(Long targetUserId, Long operatorId) {
        User operator = userRepository.findById(operatorId).orElse(null);
        if (!RoleConstants.isAdmin(operator)) {
            return Result.error(403, "只有管理员才能任命监督");
        }
        User target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) return Result.error(404, "用户不存在");
        if (targetUserId.equals(operatorId)) return Result.error(400, "不能任命自己为监督");
        if (RoleConstants.isAdmin(target)) return Result.error(400, "不能对管理员执行此操作");
        if (target.getIsBot() != null && target.getIsBot() == 1) return Result.error(400, "AI角色不能设为监督");
        if (RoleConstants.isSupervisor(target)) return Result.success("该用户已是监督");
        target.setRole(RoleConstants.SUPERVISOR);
        userRepository.save(target);
        return Result.success("已任命为监督");
    }

    public Result<?> removeSupervisor(Long targetUserId, Long operatorId) {
        User operator = userRepository.findById(operatorId).orElse(null);
        if (!RoleConstants.isAdmin(operator)) {
            return Result.error(403, "只有管理员才能解除监督");
        }
        User target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) return Result.error(404, "用户不存在");
        if (targetUserId.equals(operatorId)) return Result.error(400, "不能解除自己的监督");
        if (!RoleConstants.isSupervisor(target)) return Result.success("该用户不是监督");
        target.setRole(RoleConstants.USER);
        userRepository.save(target);
        return Result.success("已解除监督");
    }

    public Result<?> updateDisplayTitle(Long targetUserId, String displayTitle, Long operatorId) {
        User operator = userRepository.findById(operatorId).orElse(null);
        if (!RoleConstants.isAdmin(operator)) {
            return Result.error(403, "只有管理员可以修改公开头衔");
        }
        User target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) return Result.error(404, "用户不存在");
        if (!RoleConstants.isAdmin(target) && !RoleConstants.isSupervisor(target)) {
            return Result.error(400, "只能修改管理员或监督的公开头衔");
        }
        String normalized = displayTitle == null ? null : displayTitle.trim();
        if (normalized != null && normalized.length() > 50) {
            return Result.error(400, "公开头衔不能超过50个字符");
        }
        target.setDisplayTitle(normalized == null || normalized.isEmpty() ? null : normalized);
        userRepository.save(target);
        return Result.success("公开头衔已更新", toDTO(target));
    }

    @Transactional
    public Result<?> deleteUser(Long targetUserId, Long operatorId) {
        User operator = userRepository.findById(operatorId).orElse(null);
        if (!RoleConstants.isAdmin(operator)) {
            return Result.error(403, "只有管理员才能删除用户");
        }
        if (targetUserId.equals(operatorId)) {
            return Result.error(400, "不能删除自己");
        }
        User target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) return Result.error(404, "用户不存在");
        if (RoleConstants.isAdmin(target)) return Result.error(400, "不能删除管理员");
        if (RoleConstants.isSupervisor(target)) return Result.error(400, "不能删除监督");
        if (target.getIsBot() != null && target.getIsBot() == 1) return Result.error(400, "不能删除AI用户");
        if (!"USER".equals(target.getRole())) return Result.error(400, "只能删除普通用户");

        long postCount = postRepository.countByAuthorId(targetUserId);
        long replyCount = replyRepository.countByAuthorId(targetUserId);
        if (postCount > 0 || replyCount > 0) {
            return Result.error(400, "该用户已有帖子或评论，暂不支持直接删除");
        }

        postRepository.deleteLikesByUserId(targetUserId);
        postRepository.deleteFavoritesByUserId(targetUserId);
        replyRepository.clearReplyToUserByUserId(targetUserId);
        userRepository.delete(target);
        return Result.success("用户已删除");
    }

    public boolean isMuted(User user) {
        if (user.getMuted() == null || user.getMuted() == 0) return false;
        if (user.getMuteEndTime() != null && LocalDateTime.now().isAfter(user.getMuteEndTime())) {
            user.setMuted(0);
            user.setMuteEndTime(null);
            user.setMuteReason(null);
            userRepository.save(user);
            return false;
        }
        return true;
    }

    private UserDTO toDTO(User user) {
        boolean muted = isMuted(user);
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setDisplayTitle(user.getDisplayTitle());
        dto.setMuted(muted ? 1 : 0);
        dto.setMuteEndTime(muted ? user.getMuteEndTime() : null);
        dto.setMuteReason(muted ? user.getMuteReason() : null);
        dto.setIsBot(user.getIsBot());
        dto.setBotEnabled(user.getBotEnabled());
        dto.setPersona(user.getPersona());
        dto.setBotStyle(user.getBotStyle());
        dto.setBotPostLimitPerHour(user.getBotPostLimitPerHour());
        dto.setBotReplyPostLimitPerHour(user.getBotReplyPostLimitPerHour());
        dto.setBotReplyUserLimitPerHour(user.getBotReplyUserLimitPerHour());
        dto.setBotReplyBotLimitPerHour(user.getBotReplyBotLimitPerHour());
        dto.setBotReplyLimitPerHour(user.getBotReplyLimitPerHour());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    public static class LoginResponse {
        private String token;
        private UserDTO user;
        public LoginResponse(String token, UserDTO user) {
            this.token = token;
            this.user = user;
        }
        public String getToken() { return token; }
        public UserDTO getUser() { return user; }
    }
}
