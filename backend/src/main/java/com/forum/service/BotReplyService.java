package com.forum.service;

import com.forum.entity.Post;
import com.forum.entity.Reply;
import com.forum.entity.User;
import com.forum.repository.PostRepository;
import com.forum.repository.ReplyRepository;
import com.forum.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class BotReplyService {

    private static final Logger log = LoggerFactory.getLogger(BotReplyService.class);
    private static final int MIN_CONTENT_LENGTH = 1;
    private static final int MAX_CONTENT_LENGTH = 100;
    private static final Pattern AI_PATTERN = Pattern.compile(
            "(?i)作为AI|作为人工智能|我是[一]?个?AI|我是人工智能|" +
            "根据你的要求|希望这[能可]帮[助到]你|希望这对你有帮助|" +
            "如果你还[有需]问题|如果你还有其他问题|" +
            "以下是|总结一下|综上所述|需要注意的是|欢迎讨论|" +
            "我无法|我不能",
            Pattern.DOTALL);

    @Value("${ai.auto-reply.enabled:false}")
    private boolean enabled;

    @Value("${ai.auto-reply-post.enabled:true}")
    private boolean replyPostEnabled;

    @Value("${ai.auto-reply-user.enabled:true}")
    private boolean replyUserEnabled;

    @Value("${ai.auto-reply-bot.enabled:true}")
    private boolean replyBotEnabled;

    public boolean isAutoReplyEnabled() { return enabled; }

    @Value("${ai.auto-reply.recent-post-minutes:30}")
    private int recentPostMinutes;

    @Value("${ai.auto-reply.allow-bot-to-bot:false}")
    private boolean allowBotToBot;

    @Value("${ai.auto-reply.max-ai-replies-per-post:5}")
    private int maxAiRepliesPerPost;

    @Value("${ai.auto-reply.max-ai-chain-depth:2}")
    private int maxAiChainDepth;

    @Value("${ai.auto-reply.dedup-check-count:10}")
    private int dedupCheckCount;

    @Value("${ai.auto-reply.dedup-similarity-threshold:0.7}")
    private double dedupThreshold;

    @Value("${ai.auto-reply-bot.max-per-round:5}")
    private int maxBotReplyPerRound;

    private final Random random = new Random();

    private final PostRepository postRepository;
    private final ReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final DeepSeekService deepSeekService;

    public BotReplyService(PostRepository postRepository, ReplyRepository replyRepository,
                           UserRepository userRepository, DeepSeekService deepSeekService) {
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.userRepository = userRepository;
        this.deepSeekService = deepSeekService;
    }

    @Scheduled(fixedRateString = "${ai.auto-reply.interval-ms:300000}")
    @Transactional
    public void autoReply() {
        log.info("AI 自动回复扫描开始: master={}, post={}, user={}, bot={}, allowBotToBot={}",
                enabled, replyPostEnabled, replyUserEnabled, replyBotEnabled, allowBotToBot);
        if (!enabled) {
            log.info("AI 自动回复未开启，跳过扫描");
            return;
        }

        // 扫描1: AI 回复帖子
        if (replyPostEnabled) {
            autoReplyToPosts();
        } else {
            log.info("AI 回复帖子扫描跳过: replyPostEnabled=false");
        }

        // 扫描2: AI 回应用户
        if (replyUserEnabled) {
            autoReplyToHumanReplies();
            autoReplyToHumanFloorsOnAiPosts();
        } else {
            log.info("AI 回应用户扫描跳过: replyUserEnabled=false");
        }

        // 扫描3: AI 回复 AI
        if (replyBotEnabled) {
            autoReplyToBotReplies();
        } else {
            log.info("AI 回复AI扫描跳过: replyBotEnabled=false");
        }
    }

    private void autoReplyToPosts() {
        log.info("AI 回复帖子扫描开始");
        LocalDateTime since = LocalDateTime.now().minusMinutes(recentPostMinutes);
        List<Post> candidates = allowBotToBot
                ? postRepository.findRecentPosts(since)
                : postRepository.findRecentByNonBotAuthor(since);
        log.info("AI 回复帖子扫描: 候选帖子数={}", candidates.size());

        if (candidates.isEmpty()) return;
        List<User> bots = getAvailableBots();
        if (bots.isEmpty()) {
            log.info("AI 回复帖子跳过: 没有可用的 AI 角色");
            return;
        }
        log.info("AI 回复帖子扫描: 可用机器人数={}", bots.size());
        for (Post post : candidates) {
            processPost(post, bots);
        }
    }

    private void processPost(Post post, List<User> bots) {
        int aiReplyCount = countAiRepliesInPost(post.getId());
        if (aiReplyCount >= maxAiRepliesPerPost) {
            log.info("AI 回复跳过: 帖子 AI 回复总数达到上限, postId={}, count={}, max={}",
                    post.getId(), aiReplyCount, maxAiRepliesPerPost);
            return;
        }

        List<User> shuffled = new ArrayList<>(bots);
        Collections.shuffle(shuffled);

        for (User bot : shuffled) {
            // Decide target type: FLOOR (reply to post) or SUB_REPLY (reply to existing reply)
            boolean trySubReply = allowBotToBot && replyBotEnabled && random.nextBoolean();
            if (trySubReply) {
                if (tryReplyToExistingReply(post, bot)) {
                    return;
                }
                // Fall through to try FLOOR reply
            }

            if (tryReplyToPost(post, bot)) {
                return;
            }
        }

        log.info("AI 回复跳过: 帖子没有合适的 AI 角色可回复, postId={}", post.getId());
    }

    private boolean tryReplyToPost(Post post, User bot) {
        if (post.getAuthor().getId().equals(bot.getId())) {
            log.info("AI 回复跳过: 当前 AI 不能回复自己, bot={}, postId={}", bot.getUsername(), post.getId());
            return false;
        }
        if (!allowBotToBot && post.getAuthor().getIsBot() != null && post.getAuthor().getIsBot() == 1) {
            log.info("AI 回复跳过: 禁止 AI 回复 AI, bot={}, postId={}", bot.getUsername(), post.getId());
            return false;
        }
        if (replyRepository.existsFloorByPostIdAndAuthorId(post.getId(), bot.getId())) {
            log.info("AI 回复跳过: 当前 AI 已经回复过该帖子, bot={}, postId={}", bot.getUsername(), post.getId());
            return false;
        }

        if (!checkRateLimit(bot, "POST")) return false;

        String content = deepSeekService.generateReply(
                bot.getUsername(), bot.getPersona(),
                post.getTitle(), post.getContent(),
                post.getAuthor().getUsername(),
                post.getAuthor().getIsBot() != null && post.getAuthor().getIsBot() == 1);
        if (content == null) {
            log.info("AI 回复跳过: DeepSeek 生成失败, bot={}, postId={}", bot.getUsername(), post.getId());
            return false;
        }
        if (!isValidContent(content)) {
            int len = content == null ? 0 : content.length();
            log.info("AI 回复跳过: 内容不合格, bot={}, postId={}, contentLen={}", bot.getUsername(), post.getId(), len);
            return false;
        }
        if (isContentTooSimilar(content, bot.getId())) return false;

        Reply reply = new Reply();
        reply.setContent(content);
        reply.setAuthor(bot);
        reply.setPost(post);
        reply.setReplyType("FLOOR");
        Integer maxFloor = replyRepository.findMaxFloorNoByPostId(post.getId());
        reply.setFloorNo(maxFloor + 1);
        replyRepository.save(reply);
        log.info("AI 主楼回复成功: botUserId={}, botName={}, postId={}, parentReplyId=null, rootReplyId=null",
                bot.getId(), bot.getUsername(), post.getId());
        return true;
    }

    private boolean tryReplyToExistingReply(Post post, User bot) {
        List<Reply> activeReplies = replyRepository.findActiveRepliesByPostId(post.getId());
        if (activeReplies.isEmpty()) return false;

        int selfSkip = 0, alreadyRepliedSkip = 0, chainDepthSkip = 0, disabledSkip = 0;
        List<Reply> eligible = new ArrayList<>();
        for (Reply r : activeReplies) {
            if (r.getAuthor().getId().equals(bot.getId())) {
                selfSkip++;
                continue;
            }
            boolean rIsBot = r.getAuthor().getIsBot() != null && r.getAuthor().getIsBot() == 1;
            if (rIsBot && !replyBotEnabled) { disabledSkip++; continue; }
            if (!rIsBot && !replyUserEnabled) { disabledSkip++; continue; }
            if (replyRepository.existsByParentReplyIdAndAuthorId(r.getId(), bot.getId())) {
                alreadyRepliedSkip++;
                continue;
            }
            if (getAiChainDepth(r) >= maxAiChainDepth) {
                chainDepthSkip++;
                continue;
            }
            eligible.add(r);
        }
        if (eligible.isEmpty()) {
            log.info("AI 回复跳过: 无合适子回复目标, bot={}, postId={}, 总回复数={}, 跳过-自己={}, 已回复过={}, 深度上限={}, 开关过滤={}",
                    bot.getUsername(), post.getId(), activeReplies.size(), selfSkip, alreadyRepliedSkip, chainDepthSkip, disabledSkip);
            return false;
        }

        Reply target = eligible.get(random.nextInt(eligible.size()));

        boolean targetIsBot = target.getAuthor().getIsBot() != null && target.getAuthor().getIsBot() == 1;
        if (!checkRateLimit(bot, targetIsBot ? "BOT" : "USER")) return false;

        String content = deepSeekService.generateSubReply(
                bot.getUsername(), bot.getPersona(),
                post.getTitle(), post.getContent(),
                target.getContent(), target.getAuthor().getUsername(),
                targetIsBot);
        if (content == null) {
            log.info("AI 回复跳过: DeepSeek 生成失败, bot={}, postId={}, targetReplyId={}",
                    bot.getUsername(), post.getId(), target.getId());
            return false;
        }
        if (!isValidContent(content)) {
            int len = content == null ? 0 : content.length();
            log.info("AI 回复跳过: 内容不合格, bot={}, postId={}, targetReplyId={}, contentLen={}",
                    bot.getUsername(), post.getId(), target.getId(), len);
            return false;
        }
        if (isContentTooSimilar(content, bot.getId())) return false;

        Reply reply = new Reply();
        reply.setContent(content);
        reply.setAuthor(bot);
        reply.setPost(post);
        reply.setReplyType("SUB_REPLY");
        reply.setFloorNo(null);
        reply.setParentReply(target);
        reply.setReplyToUser(target.getAuthor());
        if (!"SUB_REPLY".equals(target.getReplyType())) {
            reply.setRootReplyId(target.getId());
        } else {
            reply.setRootReplyId(target.getRootReplyId());
        }
        replyRepository.save(reply);
        log.info("AI 楼中楼回复成功: botUserId={}, botName={}, postId={}, parentReplyId={}, rootReplyId={}",
                bot.getId(), bot.getUsername(), post.getId(), target.getId(), reply.getRootReplyId());
        return true;
    }

    private int countAiRepliesInPost(Long postId) {
        return replyRepository.countAiRepliesByPostId(postId);
    }

    private int getAiChainDepth(Reply reply) {
        int depth = 0;
        Reply current = reply;
        while (current != null) {
            User author = current.getAuthor();
            if (author != null && author.getIsBot() != null && author.getIsBot() == 1) {
                depth++;
                current = current.getParentReply();
            } else {
                break;
            }
        }
        return depth;
    }

    private boolean checkRateLimit(User bot, String type) {
        int limit;
        switch (type) {
            case "POST": limit = bot.getBotReplyPostLimitPerHour() != null ? bot.getBotReplyPostLimitPerHour() : 5; break;
            case "USER": limit = bot.getBotReplyUserLimitPerHour() != null ? bot.getBotReplyUserLimitPerHour() : 5; break;
            case "BOT": limit = bot.getBotReplyBotLimitPerHour() != null ? bot.getBotReplyBotLimitPerHour() : 3; break;
            default: limit = bot.getBotReplyLimitPerHour() != null ? bot.getBotReplyLimitPerHour() : 3;
        }
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long count;
        switch (type) {
            case "POST":
                count = replyRepository.countFloorRepliesByAuthorId(bot.getId(), oneHourAgo);
                break;
            case "USER":
                count = replyRepository.countSubRepliesToHumansByAuthorId(bot.getId(), oneHourAgo);
                break;
            case "BOT":
                count = replyRepository.countSubRepliesToBotsByAuthorId(bot.getId(), oneHourAgo);
                break;
            default:
                count = replyRepository.countByAuthorIdAndCreatedAtAfter(bot.getId(), oneHourAgo);
        }
        if (count >= limit) {
            log.info("AI 回复跳过: 频率限制 type={}, bot={}, count={}, limit={}",
                    type, bot.getUsername(), count, limit);
            return false;
        }
        return true;
    }

    private boolean isContentTooSimilar(String newContent, Long botId) {
        if (newContent == null || newContent.isBlank()) return false;
        List<String> recentContents = replyRepository.findRecentContentsByBotId(
                botId, PageRequest.of(0, dedupCheckCount));
        if (recentContents.isEmpty()) return false;

        Set<String> newWords = toWordSet(newContent);
        if (newWords.size() < 3) return false;

        for (String oldContent : recentContents) {
            Set<String> oldWords = toWordSet(oldContent);
            if (oldWords.size() < 3) continue;
            Set<String> intersection = new HashSet<>(newWords);
            intersection.retainAll(oldWords);
            double similarity = (double) intersection.size() / Math.max(newWords.size(), oldWords.size());
            if (similarity >= dedupThreshold) {
                log.info("AI 回复跳过: 内容去重(相似度={}), botId={}", String.format("%.2f", similarity), botId);
                return true;
            }
        }
        return false;
    }

    private Set<String> toWordSet(String text) {
        Set<String> set = new HashSet<>();
        if (text == null || text.isBlank()) return set;
        String cleaned = text.replaceAll("[\\p{P}\\s]+", "").trim();
        if (cleaned.length() < 2) return set;
        for (int i = 0; i < cleaned.length() - 1; i++) {
            set.add(cleaned.substring(i, i + 2));
        }
        for (int i = 0; i < cleaned.length() - 2; i++) {
            set.add(cleaned.substring(i, i + 3));
        }
        return set;
    }

    private boolean isValidContent(String content) {
        if (content == null || content.isBlank()) return false;
        if (content.length() < MIN_CONTENT_LENGTH) {
            log.debug("回复内容过短({}字): {}", content.length(), content);
            return false;
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            log.debug("回复内容过长({}字): {}", content.length(), content);
            return false;
        }
        if (AI_PATTERN.matcher(content).find()) {
            log.debug("回复内容包含助手口吻: {}", content);
            return false;
        }
        return true;
    }

    private List<User> getAvailableBots() {
        List<User> allBots = userRepository.findByIsBot(1);
        return allBots.stream()
                .filter(b -> b.getBotEnabled() != null && b.getBotEnabled() == 1)
                .filter(b -> !"ADMIN".equals(b.getRole()))
                .toList();
    }

    private void autoReplyToHumanReplies() {
        log.info("AI 回应用户扫描开始");
        if (!allowBotToBot) {
            log.info("AI 回应用户扫描跳过: allowBotToBot=false");
            return;
        }
        // Use 60-minute lookback (rate limit window) to avoid missing older human replies
        LocalDateTime since = LocalDateTime.now().minusMinutes(60);
        List<Reply> humanRepliesToBots = replyRepository.findRecentHumanRepliesToBots(since);
        log.info("AI 回应用户扫描: 候选数量={}", humanRepliesToBots.size());
        if (humanRepliesToBots.isEmpty()) return;

        for (Reply r : humanRepliesToBots) {
            log.info("AI 回应用户候选: replyId={}, authorId={}, replyToUserId={}, replyType={}, createdAt={}",
                    r.getId(), r.getAuthor().getId(), r.getReplyToUser().getId(),
                    r.getReplyType(), r.getCreatedAt());
            processHumanReplyToBot(r);
        }
        log.info("AI 回应用户扫描结束: 处理数量={}", humanRepliesToBots.size());
    }

    private void processHumanReplyToBot(Reply humanReply) {
        User bot = humanReply.getReplyToUser();
        Long replyId = humanReply.getId();
        Long authorId = humanReply.getAuthor().getId();
        Long botId = bot.getId();

        if (bot.getBotEnabled() == null || bot.getBotEnabled() != 1) {
            log.info("AI 回应用户跳过: bot未启用, replyId={}, authorId={}, botId={}, botName={}",
                    replyId, authorId, botId, bot.getUsername());
            return;
        }

        if (!checkRateLimit(bot, "USER")) {
            return;
        }

        if (replyRepository.existsByParentReplyIdAndAuthorId(replyId, botId)) {
            log.info("AI 回应用户跳过: 已回复过, replyId={}, authorId={}, botId={}, botName={}",
                    replyId, authorId, botId, bot.getUsername());
            return;
        }

        Post post = humanReply.getPost();
        String content = deepSeekService.generateSubReply(
                bot.getUsername(), bot.getPersona(),
                post.getTitle(), post.getContent(),
                humanReply.getContent(), humanReply.getAuthor().getUsername(),
                false);
        if (content == null) {
            log.info("AI 回应用户跳过: DeepSeek生成失败, replyId={}, authorId={}, botId={}, botName={}",
                    replyId, authorId, botId, bot.getUsername());
            return;
        }
        if (!isValidContent(content)) {
            int len = content == null ? 0 : content.length();
            log.info("AI 回应用户跳过: 内容不合格, replyId={}, authorId={}, botId={}, botName={}, contentLen={}",
                    replyId, authorId, botId, bot.getUsername(), len);
            return;
        }
        if (isContentTooSimilar(content, botId)) return;

        Reply reply = new Reply();
        reply.setContent(content);
        reply.setAuthor(bot);
        reply.setPost(post);
        reply.setReplyType("SUB_REPLY");
        reply.setFloorNo(null);
        reply.setParentReply(humanReply);
        reply.setReplyToUser(humanReply.getAuthor());
        if (!"SUB_REPLY".equals(humanReply.getReplyType())) {
            reply.setRootReplyId(humanReply.getId());
        } else {
            reply.setRootReplyId(humanReply.getRootReplyId());
        }
        replyRepository.save(reply);

        log.info("AI 回应用户成功: botUserId={}, botName={}, postId={}, parentReplyId={}, rootReplyId={}, targetUserId={}",
                botId, bot.getUsername(), post.getId(), replyId,
                reply.getRootReplyId(), authorId);
    }

    private void autoReplyToHumanFloorsOnAiPosts() {
        log.info("AI 回复帖子下人类评论扫描开始");
        if (!allowBotToBot) {
            log.info("AI 回复帖子下人类评论跳过: allowBotToBot=false");
            return;
        }
        LocalDateTime since = LocalDateTime.now().minusMinutes(60);
        List<Reply> candidates = replyRepository.findRecentHumanFloorsOnAiPosts(since);
        log.info("AI 回复帖子下人类评论: 候选数量={}", candidates.size());
        if (candidates.isEmpty()) return;

        for (Reply r : candidates) {
            log.info("AI 回复帖子下人类评论候选: replyId={}, postId={}, humanAuthorId={}, aiPostAuthorId={}, createdAt={}",
                    r.getId(), r.getPost().getId(), r.getAuthor().getId(), r.getPost().getAuthor().getId(), r.getCreatedAt());
            processHumanFloorOnAiPost(r);
        }
        log.info("AI 回复帖子下人类评论扫描结束: 处理数量={}", candidates.size());
    }

    private void processHumanFloorOnAiPost(Reply floorReply) {
        User aiAuthor = floorReply.getPost().getAuthor();
        Long replyId = floorReply.getId();
        Long postId = floorReply.getPost().getId();
        Long humanAuthorId = floorReply.getAuthor().getId();
        Long aiAuthorId = aiAuthor.getId();

        if (aiAuthor.getBotEnabled() == null || aiAuthor.getBotEnabled() != 1) {
            log.info("AI 回复帖子下人类评论跳过: AI帖主未启用, replyId={}, postId={}, humanAuthorId={}, aiAuthorId={}, aiName={}",
                    replyId, postId, humanAuthorId, aiAuthorId, aiAuthor.getUsername());
            return;
        }

        if (!checkRateLimit(aiAuthor, "USER")) {
            return;
        }

        if (replyRepository.existsByParentReplyIdAndAuthorId(replyId, aiAuthorId)) {
            log.info("AI 回复帖子下人类评论跳过: 已回复过, replyId={}, postId={}, humanAuthorId={}, aiAuthorId={}, aiName={}",
                    replyId, postId, humanAuthorId, aiAuthorId, aiAuthor.getUsername());
            return;
        }

        Post post = floorReply.getPost();
        String content = deepSeekService.generateSubReply(
                aiAuthor.getUsername(), aiAuthor.getPersona(),
                post.getTitle(), post.getContent(),
                floorReply.getContent(), floorReply.getAuthor().getUsername(),
                false);
        if (content == null) {
            log.info("AI 回复帖子下人类评论跳过: DeepSeek生成失败, replyId={}, postId={}, humanAuthorId={}, aiAuthorId={}, aiName={}",
                    replyId, postId, humanAuthorId, aiAuthorId, aiAuthor.getUsername());
            return;
        }
        if (!isValidContent(content)) {
            int len = content == null ? 0 : content.length();
            log.info("AI 回复帖子下人类评论跳过: 内容不合格, replyId={}, postId={}, humanAuthorId={}, aiAuthorId={}, aiName={}, contentLen={}",
                    replyId, postId, humanAuthorId, aiAuthorId, aiAuthor.getUsername(), len);
            return;
        }
        if (isContentTooSimilar(content, aiAuthorId)) return;

        Reply reply = new Reply();
        reply.setContent(content);
        reply.setAuthor(aiAuthor);
        reply.setPost(post);
        reply.setReplyType("SUB_REPLY");
        reply.setFloorNo(null);
        reply.setParentReply(floorReply);
        reply.setReplyToUser(floorReply.getAuthor());
        reply.setRootReplyId(floorReply.getId());
        replyRepository.save(reply);

        log.info("AI 回复帖子下人类评论成功: botUserId={}, botName={}, postId={}, parentReplyId={}, rootReplyId={}, targetUserId={}",
                aiAuthorId, aiAuthor.getUsername(), postId, replyId, reply.getRootReplyId(), humanAuthorId);
    }

    private void autoReplyToBotReplies() {
        log.info("AI 回复AI扫描开始");
        if (!allowBotToBot) {
            log.info("AI 回复AI扫描跳过: allowBotToBot=false");
            return;
        }
        LocalDateTime since = LocalDateTime.now().minusMinutes(60);
        List<Reply> botReplies = replyRepository.findRecentBotReplies(since);
        log.info("AI 回复AI扫描: 候选数量={}", botReplies.size());
        if (botReplies.isEmpty()) return;

        List<User> bots = getAvailableBots();
        if (bots.isEmpty()) {
            log.info("AI 回复AI扫描跳过: 没有可用的 AI 角色");
            return;
        }

        int processed = 0;
        for (Reply target : botReplies) {
            if (processed >= maxBotReplyPerRound) break;
            if (processBotReply(target, bots)) {
                processed++;
            }
        }
        log.info("AI 回复AI扫描结束: 候选总数={}, 本轮上限={}, 成功={}", botReplies.size(), maxBotReplyPerRound, processed);
    }

    private boolean processBotReply(Reply target, List<User> bots) {
        Long targetId = target.getId();
        Long targetAuthorId = target.getAuthor().getId();
        Long postId = target.getPost().getId();

        List<User> eligible = new ArrayList<>();
        for (User bot : bots) {
            if (bot.getId().equals(targetAuthorId)) continue;
            if (replyRepository.existsByParentReplyIdAndAuthorId(targetId, bot.getId())) continue;
            if (getAiChainDepth(target) >= maxAiChainDepth) continue;
            eligible.add(bot);
        }
        if (eligible.isEmpty()) {
            log.info("AI 回复AI跳过: 无合适的AI可回复, postId={}, targetReplyId={}", postId, targetId);
            return false;
        }

        User bot = eligible.get(random.nextInt(eligible.size()));

        if (!checkRateLimit(bot, "BOT")) return false;

        Post post = target.getPost();
        String content = deepSeekService.generateSubReply(
                bot.getUsername(), bot.getPersona(),
                post.getTitle(), post.getContent(),
                target.getContent(), target.getAuthor().getUsername(), true);
        if (content == null) {
            log.info("AI 回复AI跳过: DeepSeek生成失败, bot={}, postId={}, targetReplyId={}",
                    bot.getUsername(), postId, targetId);
            return false;
        }
        int len = content == null ? 0 : content.length();
        if (!isValidContent(content)) {
            log.info("AI 回复AI跳过: 内容不合格, bot={}, postId={}, targetReplyId={}, contentLen={}",
                    bot.getUsername(), postId, targetId, len);
            return false;
        }
        if (isContentTooSimilar(content, bot.getId())) return false;

        Reply reply = new Reply();
        reply.setContent(content);
        reply.setAuthor(bot);
        reply.setPost(post);
        reply.setReplyType("SUB_REPLY");
        reply.setFloorNo(null);
        reply.setParentReply(target);
        reply.setReplyToUser(target.getAuthor());
        if (!"SUB_REPLY".equals(target.getReplyType())) {
            reply.setRootReplyId(target.getId());
        } else {
            reply.setRootReplyId(target.getRootReplyId());
        }
        replyRepository.save(reply);
        log.info("AI 回复AI成功: botUserId={}, botName={}, postId={}, parentReplyId={}, rootReplyId={}, targetBotId={}",
                bot.getId(), bot.getUsername(), postId, targetId, reply.getRootReplyId(), targetAuthorId);
        return true;
    }
}
