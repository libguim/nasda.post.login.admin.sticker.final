package com.example.nasda.controller;

import com.example.nasda.domain.UserEntity;
import com.example.nasda.domain.UserRepository;
import com.example.nasda.dto.comment.CommentCreateRequestDto;
import com.example.nasda.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    // =========================
    // 로그인 사용자 정보 (SecurityUtil 대체)
    // =========================
    private String getLoginIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        if (principal == null) return null;
        if ("anonymousUser".equals(principal)) return null;

        String loginId = auth.getName();
        if (loginId == null || loginId.isBlank()) return null;

        return loginId;
    }

    private Integer getCurrentUserIdOrNull() {
        String loginId = getLoginIdOrNull();
        if (loginId == null) return null;

        return userRepository.findByLoginId(loginId)
                .map(UserEntity::getUserId)
                .orElse(null);
    }

    // =========================
    // 댓글 작성/삭제/수정
    // =========================

    /**
     * 댓글 작성
     * - 작성 후: 0페이지로 이동(최신 댓글 보이게)
     */
    @PostMapping("/comments")
    public String create(
            @Valid @ModelAttribute CommentCreateRequestDto req,
            @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        Integer currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) return "redirect:/user/login";

        commentService.createComment(req.postId(), currentUserId, req.content());

        return "redirect:/posts/" + req.postId()
                + "?page=0"
                + "&size=" + size
                + "#comments";
    }

    /**
     * 댓글 삭제
     * - 삭제 후: 현재 보고 있던 page 유지
     */
    @PostMapping("/comments/{id}/delete")
    public String delete(
            @PathVariable("id") Integer commentId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        Integer currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) return "redirect:/user/login";

        Integer postId = commentService.deleteComment(commentId, currentUserId);

        return "redirect:/posts/" + postId
                + "?page=0"
                + "&size=5"
                + "#comments";
    }

    /**
     * 댓글 수정
     * - 수정 후: 현재 보고 있던 page 유지
     */
    @PostMapping("/comments/{id}/edit")
    public String edit(
            @PathVariable("id") Integer commentId,
            @RequestParam("content") String content,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        Integer currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) return "redirect:/user/login";

        Integer postId = commentService.editComment(commentId, currentUserId, content);

        return "redirect:/posts/" + postId
                + "?page=" + page
                + "&size=" + size
                + "#comments";
    }
}
