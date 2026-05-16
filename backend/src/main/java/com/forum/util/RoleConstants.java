package com.forum.util;

import com.forum.entity.Reply;
import com.forum.entity.Post;
import com.forum.entity.User;

public final class RoleConstants {

    public static final String ADMIN = "ADMIN";
    public static final String SUPERVISOR = "SUPERVISOR";
    public static final String USER = "USER";

    private RoleConstants() {}

    public static boolean isAdmin(User user) {
        return user != null && ADMIN.equals(user.getRole());
    }

    public static boolean isSupervisor(User user) {
        return user != null && SUPERVISOR.equals(user.getRole());
    }

    public static boolean isSupervisorOrAbove(User user) {
        return isAdmin(user) || isSupervisor(user);
    }

    /**
     * 删除帖子权限：作者本人 / ADMIN / SUPERVISOR(但不能删ADMIN或同级SUPERVISOR)
     */
    public static boolean canDeletePost(User currentUser, Post post) {
        if (currentUser == null || post == null) return false;
        if (post.getAuthor().getId().equals(currentUser.getId())) return true;
        if (isAdmin(currentUser)) return true;
        if (isSupervisor(currentUser)) {
            String authorRole = post.getAuthor().getRole();
            return !ADMIN.equals(authorRole) && !SUPERVISOR.equals(authorRole);
        }
        return false;
    }

    /**
     * 删除回复权限：作者本人 / ADMIN / SUPERVISOR(但不能删ADMIN或同级SUPERVISOR的回复)
     */
    public static boolean canDeleteReply(User currentUser, Reply reply) {
        if (currentUser == null || reply == null) return false;
        if (reply.getAuthor().getId().equals(currentUser.getId())) return true;
        if (isAdmin(currentUser)) return true;
        if (isSupervisor(currentUser)) {
            String authorRole = reply.getAuthor().getRole();
            return !ADMIN.equals(authorRole) && !SUPERVISOR.equals(authorRole);
        }
        return false;
    }
}
