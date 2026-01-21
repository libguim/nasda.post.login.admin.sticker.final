package com.example.nasda.controller;

import com.example.nasda.domain.CommentEntity;
import com.example.nasda.domain.UserEntity;
import com.example.nasda.domain.UserRepository;
import com.example.nasda.dto.comment.CommentCreateRequestDto;
import com.example.nasda.dto.comment.CommentPageResponse;
import com.example.nasda.dto.comment.CommentViewDto;
import com.example.nasda.repository.CommentRepository;
import com.example.nasda.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    // =========================
    // 로그인 사용자 정보
    // =========================
    private String getLoginIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) return null;

        String loginId = auth.getName();
        return (loginId == null || loginId.isBlank()) ? null : loginId;
    }

    private Integer getCurrentUserIdOrNull() {
        String loginId = getLoginIdOrNull();
        if (loginId == null) return null;

        return userRepository.findByLoginId(loginId)
                .map(UserEntity::getUserId)
                .orElse(null);
    }

    // =========================
    // ✅ AJAX 댓글 페이징 API
    // =========================
    @GetMapping("/api/posts/{postId}/comments")
    @ResponseBody
    public CommentPageResponse getCommentsApi(
            @PathVariable Integer postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Integer currentUserId = getCurrentUserIdOrNull();

        Page<CommentViewDto> commentPage = commentService.getCommentsPage(postId, page, size, currentUserId);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

        List<CommentPageResponse.CommentItem> items =
                commentPage.getContent().stream()
                        .map(c -> new CommentPageResponse.CommentItem(
                                c.id(),
                                c.content(),
                                c.authorNickname(),
                                c.createdAt() == null ? "" : c.createdAt().format(fmt),
                                c.canEdit()
                        ))
                        .toList();

        CommentPageResponse.PageInfo pageInfo = new CommentPageResponse.PageInfo(
                commentPage.getNumber(),
                commentPage.getSize(),
                commentPage.getTotalPages(),
                commentPage.getTotalElements(),
                commentPage.hasPrevious(),
                commentPage.hasNext()
        );

        return new CommentPageResponse(items, pageInfo);
    }

    // =========================
    // ✅ 내 댓글 목록 조회(기존 유지)
    // =========================
    @GetMapping("/comments/my")
    public String myComments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            org.springframework.ui.Model model
    ) {
        Integer currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) return "redirect:/user/login";

        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        Page<CommentEntity> commentPage = commentService.findByUserId(currentUserId, pageable);

        model.addAttribute("comments", commentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", commentPage.getTotalPages());

        return "comment/my-list";
    }

    // =========================
    // 댓글 작성/삭제/수정 (기존 redirect 유지)
    // =========================
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

    @PostMapping("/comments/{id}/delete")
    public String delete(
            @PathVariable("id") Integer commentId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        Integer currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) return "redirect:/user/login";

        Integer postId = commentService.deleteComment(commentId, currentUserId);

        // ✅ 기존처럼 0으로 고정하지 말고 현재 page/size 유지
        return "redirect:/posts/" + postId
                + "?page=" + page
                + "&size=" + size
                + "#comments";
    }

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

    // (기존 goToComment 유지해도 됨)
    @GetMapping("/comments/{id}/go")
    public String goToComment(@PathVariable("id") Integer commentId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 없습니다."));

        Integer postId = comment.getPost().getPostId();
        int pageSize = 5;

        int page = commentService.getPageNumberByCommentId(postId, commentId, pageSize);

        return "redirect:/posts/" + postId + "?page=" + page + "#comment-" + commentId;
    }
}
