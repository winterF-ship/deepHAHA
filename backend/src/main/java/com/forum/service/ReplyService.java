package com.forum.service;

import com.forum.dto.*;
import com.forum.entity.*;
import com.forum.repository.*;
import com.forum.util.RoleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReplyService {

    private static final Logger log = LoggerFactory.getLogger(ReplyService.class);

    private final ReplyRepository replyRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public ReplyService(ReplyRepository replyRepository, PostRepository postRepository, UserRepository userRepository, UserService userService) {
        this.replyRepository = replyRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public Result<ReplyDTO> createReply(Long postId, ReplyRequest req, Long userId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }
        User author = userRepository.findById(userId).orElse(null);
        if (author == null) {
            return Result.error(401, "请先登录");
        }
        if (author.getIsBot() != null && author.getIsBot() == 1) {
            return Result.error(403, "AI 用户不能通过正常方式回复");
        }
        if (userService.isMuted(author)) {
            return Result.error(403, "您已被禁言，无法回复");
        }

        Reply reply = new Reply();
        reply.setContent(req.getContent());
        reply.setAuthor(author);
        reply.setPost(post);

        if (req.getParentReplyId() == null) {
            reply.setReplyType("FLOOR");
            Integer maxFloor = replyRepository.findMaxFloorNoByPostId(postId);
            reply.setFloorNo(maxFloor + 1);
        } else {
            Reply parent = replyRepository.findById(req.getParentReplyId()).orElse(null);
            if (parent == null || !parent.getPost().getId().equals(postId)) {
                return Result.error(400, "被回复的评论不属于该帖子");
            }
            if (parent.getDeleted() != null && parent.getDeleted() == 1) {
                return Result.error(400, "不能回复已删除的评论");
            }
            reply.setReplyType("SUB_REPLY");
            reply.setFloorNo(null);
            reply.setParentReply(parent);
            reply.setReplyToUser(parent.getAuthor());
            if (!"SUB_REPLY".equals(parent.getReplyType())) {
                reply.setRootReplyId(parent.getId());
            } else {
                reply.setRootReplyId(parent.getRootReplyId());
            }
        }

        reply = replyRepository.save(reply);

        log.info("创建回复: replyId={}, replyType={}, postId={}, authorId={}, parentReplyId={}, rootReplyId={}, replyToUserId={}",
                reply.getId(), reply.getReplyType(), postId, userId,
                reply.getParentReply() != null ? reply.getParentReply().getId() : null,
                reply.getRootReplyId(),
                reply.getReplyToUser() != null ? reply.getReplyToUser().getId() : null);

        return Result.success("回复成功", toDTO(reply));
    }

    public Result<?> deleteReply(Long postId, Long replyId, Long userId) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        Reply reply = replyRepository.findById(replyId).orElse(null);
        if (reply == null) {
            return Result.error(404, "评论不存在");
        }
        if (!reply.getPost().getId().equals(postId)) {
            return Result.error(400, "评论不属于该帖子");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (!RoleConstants.canDeleteReply(user, reply)) {
            log.warn("删除回复权限不足: replyId={}, requesterId={}, replyAuthorId={}",
                    replyId, userId, reply.getAuthor().getId());
            return Result.error(403, "没有权限删除该评论");
        }
        if (reply.getDeleted() != null && reply.getDeleted() == 1) {
            log.info("重复删除回复，直接返回成功: replyId={}, postId={}, deleteBy={}", replyId, postId, userId);
            return Result.success("删除成功");
        }

        boolean isFloor = !"SUB_REPLY".equals(reply.getReplyType());
        softDelete(reply, userId);
        log.info("删除回复: replyId={}, replyType={}, postId={}, deleteBy={}",
                replyId, reply.getReplyType(), postId, userId);

        if (isFloor) {
            List<Reply> allReplies = replyRepository.findByPostIdOrderByCreatedAtAsc(postId);
            int cascaded = 0;
            for (Reply r : allReplies) {
                if ("SUB_REPLY".equals(r.getReplyType())
                        && replyId.equals(r.getRootReplyId())
                        && (r.getDeleted() == null || r.getDeleted() == 0)) {
                    softDelete(r, userId);
                    cascaded++;
                }
            }
            log.info("删除楼层级联删除子回复: floorReplyId={}, cascadedCount={}", replyId, cascaded);
        }
        return Result.success("删除成功");
    }

    private void softDelete(Reply reply, Long userId) {
        reply.setDeleted(1);
        reply.setDeletedAt(LocalDateTime.now());
        reply.setDeletedBy(userId);
        replyRepository.save(reply);
    }

    ReplyDTO toDTO(Reply reply) {
        ReplyDTO dto = new ReplyDTO();
        boolean isDeleted = reply.getDeleted() != null && reply.getDeleted() == 1;
        dto.setId(reply.getId());
        dto.setContent(isDeleted ? "" : reply.getContent());
        dto.setAuthorId(reply.getAuthor().getId());
        dto.setAuthorName(reply.getAuthor().getUsername());
        dto.setAuthorAvatar(reply.getAuthor().getAvatar());
        dto.setAuthorDisplayTitle(resolveDisplayTitle(reply.getAuthor()));
        boolean authorMuted = userService.isMuted(reply.getAuthor());
        dto.setAuthorMuted(authorMuted ? 1 : 0);
        dto.setAuthorMuteEndTime(authorMuted ? reply.getAuthor().getMuteEndTime() : null);
        dto.setAuthorMuteReason(authorMuted ? reply.getAuthor().getMuteReason() : null);
        dto.setFloorNo(reply.getFloorNo());
        dto.setAuthorRole(reply.getAuthor().getRole());
        dto.setAuthorIsBot(reply.getAuthor().getIsBot());
        if (reply.getParentReply() != null) {
            dto.setParentReplyId(reply.getParentReply().getId());
            dto.setParentAuthorName(reply.getParentReply().getAuthor().getUsername());
        }
        if (reply.getReplyToUser() != null) {
            dto.setReplyToUserId(reply.getReplyToUser().getId());
            dto.setReplyToUsername(reply.getReplyToUser().getUsername());
        }
        dto.setDeleted(reply.getDeleted());
        dto.setCreatedAt(reply.getCreatedAt());
        dto.setRootReplyId(reply.getRootReplyId());
        dto.setReplyType(reply.getReplyType());
        return dto;
    }

    public List<MessageReplyDTO> getRepliesToMe(Long userId) {
        List<Reply> replies = replyRepository.findRepliesToMe(userId);
        return replies.stream().map(reply -> {
            MessageReplyDTO dto = new MessageReplyDTO();
            dto.setReplyId(reply.getId());
            dto.setPostId(reply.getPost().getId());
            dto.setPostTitle(reply.getPost().getTitle());
            dto.setFromUserId(reply.getAuthor().getId());
            dto.setFromUsername(reply.getAuthor().getUsername());
            dto.setFromAvatar(reply.getAuthor().getAvatar());
            dto.setFromIsBot(reply.getAuthor().getIsBot());
            dto.setReplyType(reply.getReplyType());
            dto.setContent(reply.getContent());
            dto.setCreatedAt(reply.getCreatedAt());
            dto.setParentReplyId(reply.getParentReply() != null ? reply.getParentReply().getId() : null);
            dto.setRootReplyId(reply.getRootReplyId());
            return dto;
        }).toList();
    }

    private String resolveDisplayTitle(User user) {
        if (user.getDisplayTitle() != null && !user.getDisplayTitle().isBlank()) {
            return user.getDisplayTitle();
        }
        if (RoleConstants.ADMIN.equals(user.getRole())) return "管理员";
        if (RoleConstants.SUPERVISOR.equals(user.getRole())) return "监督";
        return null;
    }
}
