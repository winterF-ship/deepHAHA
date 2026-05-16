package com.forum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.config.DeepSeekConfig;
import com.forum.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private final DeepSeekConfig config;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DeepSeekService(DeepSeekConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public String generateReply(String botUsername, String persona, String postTitle, String postContent,
                                String postAuthorName, boolean postAuthorIsBot) {
        if (config.getKey() == null || config.getKey().isBlank()) {
            log.warn("DeepSeek API Key 未配置，跳过自动回复");
            return null;
        }

        String systemPrompt = buildReplySystemPrompt(botUsername, persona);
        String userPrompt = buildReplyUserPrompt(postTitle, postContent, postAuthorName, postAuthorIsBot);

        try {
            Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.9,
                "max_tokens", 512
            );

            var responseEntity = restClient.post()
                    .uri(config.getUrl())
                    .header("Authorization", "Bearer " + config.getKey())
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);

            if (responseEntity.getStatusCode().isError()) {
                log.error("DeepSeek API error: status={}", responseEntity.getStatusCode().value());
                return null;
            }

            String response = responseEntity.getBody();
            String rawText = extractMessageContent(response);
            if (rawText == null || rawText.isBlank()) {
                return null;
            }
            return rawText.trim();

        } catch (Exception e) {
            log.error("DeepSeek reply generation failed", e);
            return null;
        }
    }

    private String buildReplySystemPrompt(String botUsername, String persona) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是「").append(botUsername).append("」，一个真实的论坛用户。你来到论坛就是为了聊天吹水、发表看法。\n");
        if (persona != null && !persona.isBlank()) {
            sb.append("你的人物设定：").append(persona).append("\n");
            sb.append("你说的每一句话都要符合这个人设——用词、态度、情绪都要吻合。\n");
        }
        sb.append("\n");
        sb.append("=== 回复规则 ===\n");
        sb.append("1. 字数1到100字（中文）。太长像写作文。\n");
        sb.append("2. 像一个普通网友一样接话——可以赞同、吐槽、抬杠、补充、感慨，但不要像客服。\n");
        sb.append("3. 说自己的看法，不要复述或总结别人的内容。\n");
        sb.append("4. 不要在结尾提问。陈述观点就够了。\n");
        sb.append("5. 纯文本，禁止Markdown、禁止换行符、禁止列表格式。\n");
        sb.append("6. 只输出回复正文本身，不要加前缀后缀、不要署名。\n");
        sb.append("\n");
        sb.append("=== 绝对禁止以下词语（出现任何一个都是违规） ===\n");
        sb.append("作为AI、我是AI、我是人工智能、根据你的要求、希望这能帮助你、\n");
        sb.append("如果你还有问题、以下是、总结一下、综上所述、我无法、我不能\n");
        return sb.toString();
    }

    private String buildReplyUserPrompt(String postTitle, String postContent,
                                         String postAuthorName, boolean postAuthorIsBot) {
        String authorDesc = postAuthorIsBot
                ? "发帖人「" + postAuthorName + "」（也是一个论坛用户）"
                : "发帖人「" + postAuthorName + "」";
        return "===== 帖子信息 =====\n" +
               "标题：" + postTitle + "\n" +
               authorDesc + "\n" +
               "正文：\n" + postContent + "\n\n" +
               "你来接个话，表达你的真实看法。不要总结、不要提问，直接输出回复正文。";
    }

    public String generateSubReply(String botUsername, String persona, String postTitle, String postContent,
                                   String parentReplyContent, String parentAuthorName, boolean parentAuthorIsBot) {
        if (config.getKey() == null || config.getKey().isBlank()) {
            log.warn("DeepSeek API Key 未配置，跳过自动回复");
            return null;
        }

        String systemPrompt = buildReplySystemPrompt(botUsername, persona);
        String userPrompt = buildSubReplyUserPrompt(postTitle, postContent, parentReplyContent, parentAuthorName, parentAuthorIsBot);

        try {
            Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.9,
                "max_tokens", 512
            );

            var responseEntity = restClient.post()
                    .uri(config.getUrl())
                    .header("Authorization", "Bearer " + config.getKey())
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);

            if (responseEntity.getStatusCode().isError()) {
                log.error("DeepSeek API error: status={}", responseEntity.getStatusCode().value());
                return null;
            }

            String response = responseEntity.getBody();
            String rawText = extractMessageContent(response);
            if (rawText == null || rawText.isBlank()) {
                return null;
            }
            return rawText.trim();

        } catch (Exception e) {
            log.error("DeepSeek sub-reply generation failed", e);
            return null;
        }
    }

    private String buildSubReplyUserPrompt(String postTitle, String postContent,
                                           String parentReplyContent, String parentAuthorName, boolean parentAuthorIsBot) {
        String targetDesc = parentAuthorIsBot
                ? "「" + parentAuthorName + "」（也是一个论坛用户，不是AI助手）"
                : "「" + parentAuthorName + "」";
        return "===== 帖子背景 =====\n" +
               "标题：" + postTitle + "\n" +
               "正文摘要：\n" + postContent + "\n\n" +
               "===== 你要回复的对象 =====\n" +
               targetDesc + " 说：\n" + parentReplyContent + "\n\n" +
               "你来接话。可以赞同、反驳、吐槽、补充——像朋友聊天一样自然。不要总结对方的话，不要提问，直接输出回复正文。";
    }

    public Result<Map<String, String>> generatePostPreview(String systemPrompt, String userPrompt) {
        if (config.getKey() == null || config.getKey().isBlank()) {
            return Result.error(503, "DeepSeek API Key 未配置");
        }

        try {
            Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.8,
                "max_tokens", 1024
            );

            var responseEntity = restClient.post()
                    .uri(config.getUrl())
                    .header("Authorization", "Bearer " + config.getKey())
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);

            if (responseEntity.getStatusCode().isError()) {
                log.error("DeepSeek API error: status={}, body={}",
                        responseEntity.getStatusCode().value(), responseEntity.getBody());
                return Result.error(500, "AI 生成失败，请重试");
            }

            String response = responseEntity.getBody();
            String rawText = extractMessageContent(response);
            Map<String, String> parsed = parseTitleAndContent(rawText);

            String title = parsed.get("title");
            String content = parsed.get("content");

            if (title.isBlank() && content.isBlank()) {
                return Result.error(500, "AI 生成失败，请重试");
            }

            return Result.success(Map.of("title", title, "content", content));

        } catch (Exception e) {
            log.error("DeepSeek API call failed", e);
            return Result.error(500, "AI 生成失败，请重试");
        }
    }

    private String extractMessageContent(String apiResponse) {
        try {
            JsonNode root = objectMapper.readTree(apiResponse);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    return message.get("content").asText().trim();
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse DeepSeek response", e);
        }
        return "";
    }

    private Map<String, String> parseTitleAndContent(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Map.of("title", "", "content", "");
        }

        String cleaned = rawText.trim();

        // Strip markdown code fences
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("```(?:json)?\\s*", "");
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```")).trim();
            }
        }

        // Try JSON parse first
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            String title = node.has("title") ? node.get("title").asText() : "";
            String content = node.has("content") ? node.get("content").asText() : "";
            if (!title.isBlank() || !content.isBlank()) {
                return Map.of("title", title, "content", content);
            }
        } catch (Exception ignored) {
            // JSON parse failed, fall through to heuristic
        }

        // Fallback: heuristic extraction
        String title = "";
        String content = cleaned;

        // Try "标题：xxx" pattern
        String[] lines = cleaned.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("标题：") || line.startsWith("标题:")) {
                title = line.replaceFirst("标题[：:]\\s*", "").trim();
                // Remove surrounding quotes if present
                if (title.startsWith("\"") && title.endsWith("\"")) {
                    title = title.substring(1, title.length() - 1);
                }
                // Content is everything after this line
                StringBuilder sb = new StringBuilder();
                for (int j = i + 1; j < lines.length; j++) {
                    String l = lines[j].trim();
                    if (l.startsWith("正文：") || l.startsWith("正文:") || l.startsWith("内容：") || l.startsWith("内容:")) {
                        l = l.replaceFirst("(?:正文|内容)[：:]\\s*", "").trim();
                    }
                    if (!l.isBlank()) {
                        sb.append(l).append("\n");
                    }
                }
                content = sb.toString().trim();
                break;
            }
        }

        // If no explicit title found, try first line as title
        if (title.isBlank() && lines.length > 0) {
            String first = lines[0].trim();
            // Remove common AI prefixes
            first = first.replaceFirst("^(?:好的[，,]|让我来写|我来发|发个帖[子子]?[：:]?)", "").trim();
            // Remove surrounding quotes
            if (first.startsWith("\"") && first.endsWith("\"")) {
                first = first.substring(1, first.length() - 1);
            }
            // Use as title if reasonable length
            if (first.length() <= 40) {
                title = first;
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < lines.length; i++) {
                    String l = lines[i].trim();
                    if (!l.isBlank()) {
                        sb.append(l).append("\n");
                    }
                }
                String rest = sb.toString().trim();
                if (!rest.isBlank()) {
                    content = rest;
                }
            }
        }

        // Strip AI-assistant language from content
        content = stripAiPhrases(content);

        // Strip markdown from content
        content = content.replaceAll("(?m)^[#*>-]+\\s*", "");
        content = content.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        content = content.replaceAll("\\*([^*]+)\\*", "$1");
        content = content.replaceAll("`([^`]+)`", "$1");

        return Map.of("title", title.trim(), "content", content.trim());
    }

    private String stripAiPhrases(String text) {
        if (text == null) return "";
        return text
            .replaceAll("(?i)作为AI[，,]?|作为人工智能[，,]?", "")
            .replaceAll("(?i)我是[一]?个?AI[^，,。\\n]*[。\\n]?", "")
            .replaceAll("(?i)根据你的要求[，,]?", "")
            .replaceAll("(?i)希望这[能可]帮[助到]你[！!。.]?", "")
            .replaceAll("(?i)如果你还[有需]问题[，,].*?[。.]?", "")
            .replaceAll("(?i)以下是[：:]?.+?[：:]?", "")
            .replaceAll("(?i)总结一下[：:]?", "")
            .replaceAll("(?i)综上所述[，,]?", "")
            .replaceAll("(?i)需要注意的是[：:,]?", "")
            .replaceAll("(?i)欢迎讨论[！!。.]?", "")
            .replaceAll("(?i)大家好[，,]我是.*?[。\\n]", "")
            .trim();
    }
}
