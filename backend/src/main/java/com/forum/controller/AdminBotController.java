package com.forum.controller;

import com.forum.dto.*;
import com.forum.entity.Category;
import com.forum.entity.User;
import com.forum.repository.CategoryRepository;
import com.forum.repository.UserRepository;
import com.forum.service.DeepSeekService;
import com.forum.service.PostService;
import com.forum.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/bots")
public class AdminBotController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final DeepSeekService deepSeekService;
    private final PostService postService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminBotController(UserRepository userRepository, CategoryRepository categoryRepository,
                              UserService userService, DeepSeekService deepSeekService,
                              PostService postService) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
        this.deepSeekService = deepSeekService;
        this.postService = postService;
    }

    @GetMapping
    public Result<List<UserDTO>> listBots(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null || (!"ADMIN".equals(currentUser.getRole()) && !"SUPERVISOR".equals(currentUser.getRole()))) {
            return Result.error(403, "没有权限执行此操作");
        }
        List<UserDTO> bots = userRepository.findByIsBot(1).stream()
                .map(u -> userService.getCurrentUser(u.getId()))
                .collect(Collectors.toList());
        return Result.success(bots);
    }

    @PostMapping
    public Result<UserDTO> createBot(@Valid @RequestBody BotCreateRequest req, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        User operator = userRepository.findById(operatorId).orElse(null);
        if (operator == null || (!"ADMIN".equals(operator.getRole()) && !"SUPERVISOR".equals(operator.getRole()))) {
            return Result.error(403, "没有权限执行此操作");
        }
        if ("清水响芙蓉".equals(req.getUsername())) {
            return Result.error(400, "不能使用此用户名");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            return Result.error(400, "用户名已存在");
        }
        User bot = new User();
        bot.setUsername(req.getUsername());
        bot.setPassword(encoder.encode(UUID.randomUUID().toString()));
        bot.setRole("USER");
        bot.setIsBot(1);
        bot.setBotEnabled(1);
        bot.setAvatar(req.getAvatar() != null ? req.getAvatar() : "");
        bot.setPersona(req.getPersona());
        bot.setBotStyle(req.getBotStyle());
        int postLimit = req.getBotPostLimitPerHour() != null ? req.getBotPostLimitPerHour() : 3;
        int replyPostLimit = req.getBotReplyPostLimitPerHour() != null ? req.getBotReplyPostLimitPerHour() : 5;
        int replyUserLimit = req.getBotReplyUserLimitPerHour() != null ? req.getBotReplyUserLimitPerHour() : 5;
        int replyBotLimit = req.getBotReplyBotLimitPerHour() != null ? req.getBotReplyBotLimitPerHour() : 3;
        if (postLimit < 0 || replyPostLimit < 0 || replyUserLimit < 0 || replyBotLimit < 0) {
            return Result.error(400, "频率限制不能小于0");
        }
        bot.setBotPostLimitPerHour(postLimit);
        bot.setBotReplyPostLimitPerHour(replyPostLimit);
        bot.setBotReplyUserLimitPerHour(replyUserLimit);
        bot.setBotReplyBotLimitPerHour(replyBotLimit);
        bot.setBotReplyLimitPerHour(3);
        bot = userRepository.save(bot);
        return Result.success("AI角色创建成功", userService.getCurrentUser(bot.getId()));
    }

    @PutMapping("/{id}")
    public Result<UserDTO> updateBot(@PathVariable Long id, @RequestBody BotUpdateRequest req, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        User operator = userRepository.findById(operatorId).orElse(null);
        if (operator == null || (!"ADMIN".equals(operator.getRole()) && !"SUPERVISOR".equals(operator.getRole()))) {
            return Result.error(403, "没有权限执行此操作");
        }
        User bot = userRepository.findById(id).orElse(null);
        if (bot == null) {
            return Result.error(404, "用户不存在");
        }
        if (bot.getIsBot() == null || bot.getIsBot() != 1) {
            return Result.error(400, "只能修改AI角色，不能修改真人用户");
        }
        if (req.getUsername() != null) {
            if ("清水响芙蓉".equals(req.getUsername())) {
                return Result.error(400, "不能使用此用户名");
            }
            if (!req.getUsername().equals(bot.getUsername()) && userRepository.existsByUsername(req.getUsername())) {
                return Result.error(400, "用户名已存在");
            }
            bot.setUsername(req.getUsername());
        }
        if (req.getAvatar() != null) {
            bot.setAvatar(req.getAvatar());
        }
        if (req.getPersona() != null) {
            bot.setPersona(req.getPersona());
        }
        if (req.getBotStyle() != null) {
            bot.setBotStyle(req.getBotStyle());
        }
        if (req.getBotEnabled() != null) {
            bot.setBotEnabled(req.getBotEnabled());
        }
        if (req.getBotPostLimitPerHour() != null) {
            if (req.getBotPostLimitPerHour() < 0) {
                return Result.error(400, "发帖限制不能小于0");
            }
            bot.setBotPostLimitPerHour(req.getBotPostLimitPerHour());
        }
        if (req.getBotReplyPostLimitPerHour() != null) {
            if (req.getBotReplyPostLimitPerHour() < 0) return Result.error(400, "回复帖子限制不能小于0");
            bot.setBotReplyPostLimitPerHour(req.getBotReplyPostLimitPerHour());
        }
        if (req.getBotReplyUserLimitPerHour() != null) {
            if (req.getBotReplyUserLimitPerHour() < 0) return Result.error(400, "回复用户限制不能小于0");
            bot.setBotReplyUserLimitPerHour(req.getBotReplyUserLimitPerHour());
        }
        if (req.getBotReplyBotLimitPerHour() != null) {
            if (req.getBotReplyBotLimitPerHour() < 0) return Result.error(400, "回复AI限制不能小于0");
            bot.setBotReplyBotLimitPerHour(req.getBotReplyBotLimitPerHour());
        }
        if (req.getBotReplyLimitPerHour() != null) {
            if (req.getBotReplyLimitPerHour() < 0) return Result.error(400, "回复限制不能小于0");
            bot.setBotReplyLimitPerHour(req.getBotReplyLimitPerHour());
        }
        bot.setRole("USER");
        bot = userRepository.save(bot);
        return Result.success("AI角色信息已更新", userService.getCurrentUser(bot.getId()));
    }

    @PostMapping("/{id}/generate-post-preview")
    public Result<Map<String, String>> generatePostPreview(@PathVariable Long id,
                                                           @Valid @RequestBody GeneratePostRequest req,
                                                           HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        User operator = userRepository.findById(operatorId).orElse(null);
        if (operator == null || (!"ADMIN".equals(operator.getRole()) && !"SUPERVISOR".equals(operator.getRole()))) {
            return Result.error(403, "没有权限执行此操作");
        }

        User bot = userRepository.findById(id).orElse(null);
        if (bot == null) {
            return Result.error(404, "用户不存在");
        }
        if (bot.getIsBot() == null || bot.getIsBot() != 1) {
            return Result.error(400, "该用户不是AI角色");
        }
        if (bot.getBotEnabled() == null || bot.getBotEnabled() != 1) {
            return Result.error(400, "该AI角色未启用");
        }
        if (!"USER".equals(bot.getRole())) {
            return Result.error(400, "AI角色不能是管理员");
        }

        String categoryContext = "";
        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId()).orElse(null);
            if (category != null) {
                categoryContext = "帖子将发布到「" + category.getName() + "」板块";
                if (category.getDescription() != null) {
                    categoryContext += "（" + category.getDescription() + "）";
                }
                categoryContext += "。请撰写与该板块主题相符的内容。\n";
            }
        }

        String persona = bot.getPersona();
        String systemPrompt = buildSystemPrompt(persona);
        String userPrompt = categoryContext
                + "话题：" + req.getTopic() + "\n\n"
                + "围绕以上话题写一篇论坛帖子。\n\n"
                + "要求：\n"
                + "1. 正文 100-300 字，像真人闲聊，不要长篇大论。\n"
                + "2. 标题简短抓眼，10-20 字即可。\n"
                + "3. 口吻自然，不要正式、不要官腔、不要总结。\n"
                + "4. 不要使用 Markdown 格式（不要 # ## - * > 等符号）。\n"
                + "5. 严格输出以下 JSON，不要输出任何其他内容：\n"
                + "{\"title\":\"帖子标题\",\"content\":\"帖子正文\"}";

        return deepSeekService.generatePostPreview(systemPrompt, userPrompt);
    }

    @PostMapping("/{id}/publish-post")
    public Result<PostDTO> publishPost(@PathVariable Long id,
                                        @Valid @RequestBody PostRequest req,
                                        HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        User operator = userRepository.findById(operatorId).orElse(null);
        if (operator == null || (!"ADMIN".equals(operator.getRole()) && !"SUPERVISOR".equals(operator.getRole()))) {
            return Result.error(403, "没有权限执行此操作");
        }

        User bot = userRepository.findById(id).orElse(null);
        if (bot == null) {
            return Result.error(404, "用户不存在");
        }
        if (bot.getIsBot() == null || bot.getIsBot() != 1) {
            return Result.error(400, "该用户不是AI角色");
        }
        if (bot.getBotEnabled() == null || bot.getBotEnabled() != 1) {
            return Result.error(400, "该AI角色未启用");
        }
        if (!"USER".equals(bot.getRole())) {
            return Result.error(400, "AI角色不能是管理员");
        }

        return postService.createPost(req, bot.getId());
    }

    @PostMapping("/upload-avatar")
    public Result<Map<String, String>> uploadBotAvatar(@RequestParam("file") MultipartFile file,
                                                        HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        User operator = userRepository.findById(operatorId).orElse(null);
        if (operator == null || (!"ADMIN".equals(operator.getRole()) && !"SUPERVISOR".equals(operator.getRole()))) {
            return Result.error(403, "没有权限执行此操作");
        }
        if (file.isEmpty()) {
            return Result.error(400, "请选择文件");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error(400, "文件不能超过2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只支持图片文件");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String filename = "bot_avatar_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        try {
            Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads", "avatars");
            Files.createDirectories(uploadPath);
            file.transferTo(uploadPath.resolve(filename).toFile());
        } catch (Exception e) {
            return Result.error(500, "文件上传失败");
        }
        String avatarUrl = "/uploads/avatars/" + filename;
        Map<String, String> result = new HashMap<>();
        result.put("avatar", avatarUrl);
        return Result.success("上传成功", result);
    }

    private String buildSystemPrompt(String persona) {
        String base;
        if (persona != null && !persona.isBlank()) {
            base = "你是论坛里的一个普通用户。你的性格和说话风格：" + persona + "。";
        } else {
            base = "你是论坛里的一个普通用户，喜欢闲聊和分享观点。";
        }
        base += " 你不是AI助手。你必须像一个真实的人一样说话——有自己的情绪、观点、偏好、偶尔吐槽。\n\n"
                + "严格禁止以下表述：\n"
                + "- 绝对不能说\"作为AI\"\"作为人工智能\"\"我是AI\"\"我是一个人工智能\"\n"
                + "- 绝对不能说\"根据你的要求\"\"希望这能帮助你\"\"如果你还有问题\"\n"
                + "- 绝对不能说\"以下是\"\"总结一下\"\"综上所述\"\"需要注意的是\"\n"
                + "- 不要用\"你好！\"开头，不要用\"欢迎讨论\"结尾\n"
                + "- 不要列出1. 2. 3.，不要写要点清单\n"
                + "- 就像你在刷论坛时随手打的一段话\n\n"
                + "你只输出 JSON。你从不解释自己，你只是发帖。";
        return base;
    }
}
