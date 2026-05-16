package com.forum.service;

import com.forum.config.DeepSeekConfig;
import com.forum.dto.PostDTO;
import com.forum.dto.PostRequest;
import com.forum.dto.Result;
import com.forum.entity.Category;
import com.forum.entity.User;
import com.forum.repository.CategoryRepository;
import com.forum.repository.PostRepository;
import com.forum.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class AiAutoPostService {

    private static final Logger log = LoggerFactory.getLogger(AiAutoPostService.class);
    private static final Pattern ADMIN_IMPERSONATION = Pattern.compile(
            "(?i)公告|通知|系统|管理员|版规|删帖|封禁|禁言|违规|处罚|官方|管理团队|升级维护|迁移|备份",
            Pattern.DOTALL);

    // ---- defaults from application.properties ----
    @Value("${ai.auto-post.enabled:false}")
    private boolean propEnabled;

    @Value("${ai.auto-post.category-ids:}")
    private String propCategoryIds;

    @Value("${ai.auto-post.user-ids:}")
    private String propUserIds;

    @Value("${ai.auto-post.topic-style:}")
    private String propTopicStyle;

    @Value("${ai.auto-post.min-title-length:5}")
    private int propMinTitleLength;

    @Value("${ai.auto-post.max-title-length:40}")
    private int propMaxTitleLength;

    @Value("${ai.auto-post.min-content-length:50}")
    private int propMinContentLength;

    @Value("${ai.auto-post.max-content-length:500}")
    private int propMaxContentLength;

    @Value("${ai.auto-post.interval-ms:600000}")
    private long propIntervalMs;

    // ---- runtime overrides (set by admin API, lost on restart) ----
    private Boolean runtimeEnabled;
    private String runtimeCategoryIds;
    private String runtimeUserIds;
    private String runtimeTopicStyle;
    private Integer runtimeMinTitleLength;
    private Integer runtimeMaxTitleLength;
    private Integer runtimeMinContentLength;
    private Integer runtimeMaxContentLength;

    private final Random random = new Random();
    private final DeepSeekService deepSeekService;
    private final DeepSeekConfig deepSeekConfig;
    private final PostService postService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    public AiAutoPostService(DeepSeekService deepSeekService, DeepSeekConfig deepSeekConfig,
                             PostService postService, UserRepository userRepository,
                             CategoryRepository categoryRepository, PostRepository postRepository) {
        this.deepSeekService = deepSeekService;
        this.deepSeekConfig = deepSeekConfig;
        this.postService = postService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
    }

    @Scheduled(fixedRateString = "${ai.auto-post.interval-ms:600000}")
    @Transactional
    public void autoPost() {
        if (!isEnabled()) {
            log.info("AI 自动发帖未开启，跳过");
            return;
        }
        if (deepSeekConfig.getKey() == null || deepSeekConfig.getKey().isBlank()) {
            log.warn("DeepSeek API Key 为空，AI 自动发帖跳过");
            return;
        }

        List<User> bots = getAvailableBots();
        if (bots.isEmpty()) {
            log.info("未配置 AI 发帖用户或没有可用的 AI 用户，跳过");
            return;
        }

        List<Category> categories = getAvailableCategories();
        if (categories.isEmpty()) {
            log.info("未配置 AI 发帖分类或没有可用分类，跳过");
            return;
        }

        Collections.shuffle(bots);
        for (User bot : bots) {
            if (!checkRateLimit(bot)) {
                continue;
            }
            Category category = categories.get(random.nextInt(categories.size()));

            String systemPrompt = buildSystemPrompt(bot);
            String userPrompt = buildUserPrompt(bot, category);

            Result<Map<String, String>> genResult = deepSeekService.generatePostPreview(systemPrompt, userPrompt);
            if (genResult.getCode() != 200 || genResult.getData() == null) {
                log.info("AI 自动发帖跳过: DeepSeek 生成失败, bot={}, category={}", bot.getUsername(), category.getName());
                continue;
            }

            String title = genResult.getData().get("title");
            String content = genResult.getData().get("content");

            if (title == null || title.isBlank()) {
                log.info("AI 自动发帖跳过: DeepSeek 返回标题为空, bot={}", bot.getUsername());
                continue;
            }
            if (content == null || content.isBlank()) {
                log.info("AI 自动发帖跳过: DeepSeek 返回内容为空, bot={}", bot.getUsername());
                continue;
            }

            title = title.trim();
            content = content.trim();

            int minTitle = getEffectiveMinTitleLength();
            int maxTitle = getEffectiveMaxTitleLength();
            int minContent = getEffectiveMinContentLength();
            int maxContent = getEffectiveMaxContentLength();

            if (title.length() < minTitle) {
                log.info("AI 自动发帖跳过: 标题过短({}字 < {}), bot={}", title.length(), minTitle, bot.getUsername());
                continue;
            }
            if (title.length() > maxTitle) {
                title = title.substring(0, maxTitle);
            }
            if (content.length() < minContent) {
                log.info("AI 自动发帖跳过: 内容过短({}字 < {}), bot={}", content.length(), minContent, bot.getUsername());
                continue;
            }
            if (content.length() > maxContent) {
                content = content.substring(0, maxContent);
            }

            if (looksLikeAdminContent(title, content)) {
                log.info("AI 自动发帖跳过: 内容疑似管理员公告/系统通知, bot={}, title={}", bot.getUsername(), title);
                continue;
            }

            PostRequest postReq = new PostRequest();
            postReq.setTitle(title);
            postReq.setContent(content);
            postReq.setCategoryId(category.getId());

            Result<PostDTO> postResult = postService.createPost(postReq, bot.getId());
            if (postResult.getCode() == 200 && postResult.getData() != null) {
                log.info("AI 自动发帖成功: postId={}, userId={}, botName={}, categoryId={}",
                        postResult.getData().getId(), bot.getId(), bot.getUsername(), category.getId());
            } else {
                log.warn("AI 自动发帖失败: bot={}, message={}", bot.getUsername(), postResult.getMessage());
            }
            return;
        }

        log.info("AI 自动发帖本轮无可用 AI: 全部被频率限制或其他条件过滤");
    }

    private List<User> getAvailableBots() {
        List<User> allBots = userRepository.findByIsBot(1);
        List<Long> configuredIds = parseIdList(getEffectiveUserIds());
        return allBots.stream()
                .filter(b -> b.getBotEnabled() != null && b.getBotEnabled() == 1)
                .filter(b -> !"ADMIN".equals(b.getRole()))
                .filter(b -> configuredIds.isEmpty() || configuredIds.contains(b.getId()))
                .toList();
    }

    private List<Category> getAvailableCategories() {
        List<Long> configuredIds = parseIdList(getEffectiveCategoryIds());
        if (configuredIds.isEmpty()) {
            return categoryRepository.findAll();
        }
        List<Category> result = new ArrayList<>();
        for (Long id : configuredIds) {
            categoryRepository.findById(id).ifPresent(result::add);
        }
        return result;
    }

    private List<Long> parseIdList(String str) {
        if (str == null || str.isBlank()) return List.of();
        List<Long> result = new ArrayList<>();
        for (String part : str.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            try {
                result.add(Long.parseLong(trimmed));
            } catch (NumberFormatException e) {
                log.warn("AI 自动发帖配置中包含非法 ID，已跳过: {}", trimmed);
            }
        }
        return result;
    }

    private boolean checkRateLimit(User bot) {
        int limit = bot.getBotPostLimitPerHour() != null ? bot.getBotPostLimitPerHour() : 3;
        if (limit == 0) {
            log.info("AI 自动发帖跳过: bot={} 发帖频率限制为0", bot.getUsername());
            return false;
        }
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentPosts = postRepository.countByAuthorIdAndCreatedAtAfter(bot.getId(), oneHourAgo);
        if (recentPosts >= limit) {
            log.info("AI 自动发帖跳过: bot={} 最近1小时发帖数({})达到限制({})",
                    bot.getUsername(), recentPosts, limit);
            return false;
        }
        return true;
    }

    private String buildSystemPrompt(User bot) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是「").append(bot.getUsername()).append("」，一个真实的论坛用户。\n");
        String persona = bot.getPersona();
        if (persona != null && !persona.isBlank()) {
            sb.append("你的人设：").append(persona).append("\n");
            sb.append("你说的每一句话、发的每一个帖子都要符合这个人设。\n");
        }
        sb.append("\n");
        sb.append("=== 发帖规则 ===\n");
        sb.append("1. 像一个普通网友随手发的帖子——可以聊日常、吐槽、分享经验、讨论话题。\n");
        sb.append("2. 标题简短抓眼，正文自然流畅，不要长篇大论。\n");
        sb.append("3. 不要写作文、不要写总结报告、不要列清单。\n");
        sb.append("4. 你不是客服，不是管理员，不是AI助手——你就是个来论坛聊天的人。\n");
        sb.append("5. 纯文本，禁止Markdown、禁止换行符、禁止列表格式。\n");
        sb.append("\n");
        sb.append("=== 绝对禁止 ===\n");
        sb.append("作为AI、我是AI、作为人工智能、根据你的要求、希望这能帮助你、\n");
        sb.append("如果你还有问题、以下是、总结一下、综上所述、需要注意的是、\n");
        sb.append("公告、通知、系统、管理员、版规、删帖、封禁\n");
        sb.append("\n");
        sb.append("你只输出 JSON，不要输出任何其他内容。");
        return sb.toString();
    }

    private String buildUserPrompt(User bot, Category category) {
        String style = getEffectiveTopicStyle();
        StringBuilder sb = new StringBuilder();
        sb.append("请以「").append(bot.getUsername()).append("」的身份，在「").append(category.getName()).append("」板块发一篇帖子。\n");
        if (style != null && !style.isBlank()) {
            sb.append("话题方向：").append(style).append("\n");
        }
        sb.append("\n");
        sb.append("要求：\n");
        sb.append("1. 正文 ").append(getEffectiveMinContentLength()).append("-").append(getEffectiveMaxContentLength()).append(" 字，像真人闲聊。\n");
        sb.append("2. 标题 ").append(getEffectiveMinTitleLength()).append("-").append(getEffectiveMaxTitleLength()).append(" 字，简短抓眼。\n");
        sb.append("3. 口吻自然，不要官腔，不要AI腔。\n");
        sb.append("4. 严格输出以下 JSON 格式，不要输出任何其他内容：\n");
        sb.append("{\"title\":\"帖子标题\",\"content\":\"帖子正文\"}");
        return sb.toString();
    }

    private boolean looksLikeAdminContent(String title, String content) {
        return ADMIN_IMPERSONATION.matcher(title).find()
                || ADMIN_IMPERSONATION.matcher(content).find();
    }

    // ---- effective config: runtime override > properties default ----

    public boolean isEnabled() {
        return runtimeEnabled != null ? runtimeEnabled : propEnabled;
    }

    public String getEffectiveCategoryIds() {
        return runtimeCategoryIds != null ? runtimeCategoryIds : propCategoryIds;
    }

    public String getEffectiveUserIds() {
        return runtimeUserIds != null ? runtimeUserIds : propUserIds;
    }

    public String getEffectiveTopicStyle() {
        return runtimeTopicStyle != null ? runtimeTopicStyle : propTopicStyle;
    }

    public int getEffectiveMinTitleLength() {
        return runtimeMinTitleLength != null ? runtimeMinTitleLength : propMinTitleLength;
    }

    public int getEffectiveMaxTitleLength() {
        return runtimeMaxTitleLength != null ? runtimeMaxTitleLength : propMaxTitleLength;
    }

    public int getEffectiveMinContentLength() {
        return runtimeMinContentLength != null ? runtimeMinContentLength : propMinContentLength;
    }

    public int getEffectiveMaxContentLength() {
        return runtimeMaxContentLength != null ? runtimeMaxContentLength : propMaxContentLength;
    }

    // ---- runtime setters for admin API ----

    public void setRuntimeEnabled(Boolean v) { this.runtimeEnabled = v; }
    public void setRuntimeCategoryIds(String v) { this.runtimeCategoryIds = v; }
    public void setRuntimeUserIds(String v) { this.runtimeUserIds = v; }
    public void setRuntimeTopicStyle(String v) { this.runtimeTopicStyle = v; }
    public void setRuntimeMinTitleLength(Integer v) { this.runtimeMinTitleLength = v; }
    public void setRuntimeMaxTitleLength(Integer v) { this.runtimeMaxTitleLength = v; }
    public void setRuntimeMinContentLength(Integer v) { this.runtimeMinContentLength = v; }
    public void setRuntimeMaxContentLength(Integer v) { this.runtimeMaxContentLength = v; }

    public Map<String, Object> getConfigSnapshot() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", isEnabled());
        map.put("intervalMs", getEffectiveIntervalMs());
        map.put("intervalMsRuntime", false);
        map.put("categoryIds", getEffectiveCategoryIds());
        map.put("userIds", getEffectiveUserIds());
        map.put("topicStyle", getEffectiveTopicStyle());
        map.put("minTitleLength", getEffectiveMinTitleLength());
        map.put("maxTitleLength", getEffectiveMaxTitleLength());
        map.put("minContentLength", getEffectiveMinContentLength());
        map.put("maxContentLength", getEffectiveMaxContentLength());
        map.put("restartRequiredFor", "intervalMs 修改需重启后生效");
        map.put("runtimeChangesLostOnRestart", true);
        return map;
    }

    long getEffectiveIntervalMs() {
        return propIntervalMs;
    }
}
