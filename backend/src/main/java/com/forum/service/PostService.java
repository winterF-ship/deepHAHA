package com.forum.service;

import com.forum.dto.*;
import com.forum.entity.*;
import com.forum.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.forum.util.RoleConstants;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ReplyService replyService;
    private final ReplyRepository replyRepository;

    public PostService(PostRepository postRepository, CategoryRepository categoryRepository, UserRepository userRepository,
                       UserService userService, ReplyService replyService, ReplyRepository replyRepository) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.replyService = replyService;
        this.replyRepository = replyRepository;
    }

    public Result<PageResult<PostDTO>> getPosts(Long categoryId, int page, int size, Long userId) {
        Page<Post> postPage;
        if (categoryId != null) {
            postPage = postRepository.findByCategoryId(categoryId, PageRequest.of(page, size));
        } else {
            postPage = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        }
        List<PostDTO> list = postPage.getContent().stream().map(p -> toListDTO(p, userId)).collect(Collectors.toList());
        PageResult<PostDTO> result = new PageResult<>(
                list, postPage.getTotalElements(), postPage.getTotalPages(),
                postPage.getNumber(), postPage.getSize());
        return Result.success(result);
    }

    public Result<PostDTO> getPostDetail(Long id, Long userId) {
        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
        return Result.success(toDetailDTO(post, userId));
    }

    public Result<PostDTO> createPost(PostRequest req, Long userId) {
        User author = userRepository.findById(userId).orElse(null);
        if (author != null && userService.isMuted(author)) {
            return Result.error(403, "您已被禁言，无法发帖");
        }
        Category category = categoryRepository.findById(req.getCategoryId()).orElse(null);
        if (category == null) {
            return Result.error(400, "板块不存在");
        }
        Post post = new Post();
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        post.setAuthor(author);
        post.setCategory(category);
        post = postRepository.save(post);
        return Result.success("发帖成功", toListDTO(post, userId));
    }

    public Result<PostDTO> toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) return Result.error(404, "帖子不存在");
        User user = userRepository.findById(userId).orElse(null);
        if (post.getLikedBy().contains(user)) {
            post.getLikedBy().remove(user);
        } else {
            post.getLikedBy().add(user);
        }
        postRepository.save(post);
        return Result.success(post.getLikedBy().contains(user) ? "点赞成功" : "取消点赞", toListDTO(post, userId));
    }

    public Result<?> deletePost(Long postId, Long userId) {
        if (userId == null) return Result.error(401, "请先登录");
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) return Result.error(404, "帖子不存在");
        User user = userRepository.findById(userId).orElse(null);
        if (!RoleConstants.canDeletePost(user, post)) {
            return Result.error(403, "没有权限删除该帖子");
        }
        post.getLikedBy().clear();
        post.getFavoritedBy().clear();
        postRepository.delete(post);
        return Result.success("删除成功");
    }

    public Result<PostDTO> toggleFavorite(Long postId, Long userId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) return Result.error(404, "帖子不存在");
        User user = userRepository.findById(userId).orElse(null);
        if (post.getFavoritedBy().contains(user)) {
            post.getFavoritedBy().remove(user);
        } else {
            post.getFavoritedBy().add(user);
        }
        postRepository.save(post);
        return Result.success(post.getFavoritedBy().contains(user) ? "收藏成功" : "取消收藏", toListDTO(post, userId));
    }

    private PostDTO toListDTO(Post post, Long userId) {
        PostDTO dto = new PostDTO();
        fillDTO(dto, post, userId);
        dto.setContent(post.getContent().length() > 200 ? post.getContent().substring(0, 200) + "..." : post.getContent());
        return dto;
    }

    private PostDTO toDetailDTO(Post post, Long userId) {
        PostDTO dto = new PostDTO();
        fillDTO(dto, post, userId);
        dto.setContent(post.getContent());
        List<Reply> allReplies = post.getReplies();
        List<Reply> floors = getVisibleFloors(allReplies);
        Set<Long> visibleFloorIds = floors.stream().map(Reply::getId).collect(Collectors.toSet());
        List<Reply> subReplies = getVisibleSubReplies(allReplies, visibleFloorIds);
        dto.setReplies(floors.stream().map(floor -> {
            ReplyDTO floorDTO = replyService.toDTO(floor);
            List<ReplyDTO> children = subReplies.stream()
                    .filter(sr -> floor.getId().equals(sr.getRootReplyId()))
                    .map(replyService::toDTO)
                    .collect(Collectors.toList());
            floorDTO.setSubReplies(children);
            return floorDTO;
        }).collect(Collectors.toList()));
        return dto;
    }

    private void fillDTO(PostDTO dto, Post post, Long userId) {
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setAuthorId(post.getAuthor().getId());
        dto.setAuthorName(post.getAuthor().getUsername());
        dto.setAuthorAvatar(post.getAuthor().getAvatar());
        dto.setAuthorDisplayTitle(resolveDisplayTitle(post.getAuthor()));
        boolean authorMuted = userService.isMuted(post.getAuthor());
        dto.setAuthorMuted(authorMuted ? 1 : 0);
        dto.setAuthorMuteEndTime(authorMuted ? post.getAuthor().getMuteEndTime() : null);
        dto.setAuthorMuteReason(authorMuted ? post.getAuthor().getMuteReason() : null);
        dto.setAuthorRole(post.getAuthor().getRole());
        dto.setAuthorIsBot(post.getAuthor().getIsBot());
        dto.setCategoryId(post.getCategory().getId());
        dto.setCategoryName(post.getCategory().getName());
        dto.setViewCount(post.getViewCount());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        int visibleReplyCount = countVisibleReplies(post.getReplies());
        dto.setReplyCount(visibleReplyCount);
        logReplyCountCheck(post.getId(), visibleReplyCount);
        dto.setLikeCount(post.getLikedBy().size());
        dto.setFavoriteCount(post.getFavoritedBy().size());
        if (userId != null) {
            dto.setLiked(post.getLikedBy().stream().anyMatch(u -> u.getId().equals(userId)));
            dto.setFavorited(post.getFavoritedBy().stream().anyMatch(u -> u.getId().equals(userId)));
        }
    }

    private int countVisibleReplies(List<Reply> replies) {
        List<Reply> floors = getVisibleFloors(replies);
        Set<Long> visibleFloorIds = floors.stream().map(Reply::getId).collect(Collectors.toSet());
        return floors.size() + getVisibleSubReplies(replies, visibleFloorIds).size();
    }

    private List<Reply> getVisibleFloors(List<Reply> replies) {
        return replies.stream()
                .filter(r -> isActive(r) && !"SUB_REPLY".equals(r.getReplyType()))
                .sorted((a, b) -> {
                    int fa = a.getFloorNo() != null ? a.getFloorNo() : 0;
                    int fb = b.getFloorNo() != null ? b.getFloorNo() : 0;
                    return Integer.compare(fa, fb);
                })
                .collect(Collectors.toList());
    }

    private List<Reply> getVisibleSubReplies(List<Reply> replies, Set<Long> visibleFloorIds) {
        return replies.stream()
                .filter(r -> isActive(r)
                        && "SUB_REPLY".equals(r.getReplyType())
                        && r.getParentReply() != null
                        && r.getRootReplyId() != null
                        && visibleFloorIds.contains(r.getRootReplyId()))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private boolean isActive(Reply reply) {
        return reply.getDeleted() == null || reply.getDeleted() == 0;
    }

    private void logReplyCountCheck(Long postId, int visibleReplyCount) {
        long allCount = replyRepository.countAllByPostId(postId);
        long activeCount = replyRepository.countActiveByPostId(postId);
        long activeFloorCount = replyRepository.countActiveFloorByPostId(postId);
        long activeSubReplyCount = replyRepository.countActiveSubReplyByPostId(postId);
        long orphanSubReplyCount = replyRepository.countActiveOrphanSubReplyByPostId(postId);
        log.info("reply-count-check postId={}, post.repliesCount={}, all={}, active={}, floorActive={}, subReplyActive={}, orphanSubReplyActive={}",
                postId, visibleReplyCount, allCount, activeCount, activeFloorCount, activeSubReplyCount, orphanSubReplyCount);
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
