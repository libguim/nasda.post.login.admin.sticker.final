package com.example.nasda.controller;

import com.example.nasda.domain.CategoryEntity;
import com.example.nasda.domain.PostEntity;
import com.example.nasda.domain.UserEntity;
import com.example.nasda.domain.UserRepository;
import com.example.nasda.dto.post.PostCreateRequestDto;
import com.example.nasda.dto.post.PostViewDto;
import com.example.nasda.repository.CategoryRepository;
import com.example.nasda.service.CommentService;
import com.example.nasda.service.PostImageService;
import com.example.nasda.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CategoryRepository categoryRepository;
    private final CommentService commentService;
    private final PostImageService postImageService;
    private final UserRepository userRepository;

    // =========================
    // 0) 홈 "/" = 게시글 목록
    // =========================
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("posts", postService.getHomePosts());

        String nickname = getCurrentNicknameOrNull();
        model.addAttribute("username", nickname == null ? "게스트" : nickname);

        return "index";
    }

    // =========================
    // 0-1) /posts 접속은 홈으로 보내서 사실상 /posts 라우트 없애기
    // =========================
    @GetMapping("/posts")
    public String postsRedirect() {
        return "redirect:/";
    }

    // =========================
    // 1) 글 작성 페이지 (GET)
    // =========================
    @GetMapping("/posts/create")
    public String createForm(Model model) {
        model.addAttribute("postCreateRequestDto", new PostCreateRequestDto("", "", ""));
        model.addAttribute("categories", categoryRepository.findAll());

        String nickname = getCurrentNicknameOrNull();
        model.addAttribute("username", nickname == null ? "게스트" : nickname);

        return "post/create";
    }

    // =========================
    // 2) 게시글 상세 보기
    // =========================
    @GetMapping("/posts/{postId}")
    public String viewPost(
            @PathVariable("postId") String postIdStr,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model
    ) {
        try {
            // 혹시 /posts/create 같은 것이 여기로 들어오면 create로 보냄
            if ("create".equals(postIdStr)) return "redirect:/posts/create";

            Integer postId = Integer.parseInt(postIdStr);

            PostEntity entity = postService.get(postId);
            List<String> imageUrls = postImageService.getImageUrls(postId);
            Integer currentUserId = getCurrentUserIdOrNull();

            boolean isOwner = currentUserId != null
                    && entity.getUser() != null
                    && currentUserId.equals(entity.getUser().getUserId());

            PostViewDto post = new PostViewDto(
                    entity.getPostId(),
                    entity.getTitle(),
                    entity.getDescription(),
                    entity.getCategory().getCategoryName(),
                    new PostViewDto.AuthorDto(entity.getUser().getNickname()),
                    imageUrls,
                    entity.getCreatedAt(),
                    isOwner
            );

            var commentsPage = commentService.getCommentsPage(postId, page, size, currentUserId);

            model.addAttribute("post", post);
            model.addAttribute("comments", commentsPage.getContent());
            model.addAttribute("commentsPage", commentsPage);

            String nickname = getCurrentNicknameOrNull();
            model.addAttribute("username", nickname == null ? "게스트" : nickname);

            return "post/view";
        } catch (NumberFormatException e) {
            return "redirect:/";
        }
    }

    // =========================
    // 3) 글 등록(POST)
    // =========================
    @PostMapping("/posts")
    public String createPost(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        Integer userId = getCurrentUserIdOrNull();
        if (userId == null) return "redirect:/user/login";

        CategoryEntity categoryEntity = categoryRepository.findByCategoryName(category)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리: " + category));

        PostEntity post = postService.create(userId, categoryEntity.getCategoryId(), title, description);

        if (images != null && !images.isEmpty()) {
            postImageService.addImages(post, images);
        }

        return "redirect:/posts/" + post.getPostId();
    }

    // =========================
    // 4) 수정 페이지 (GET)
    // =========================
    @GetMapping("/posts/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        PostEntity entity = postService.get(id);

        model.addAttribute("postId", entity.getPostId());
        model.addAttribute("title", entity.getTitle());
        model.addAttribute("description", entity.getDescription());
        model.addAttribute("category", entity.getCategory().getCategoryName());
        model.addAttribute("images", List.of()); // 기존 로직 유지
        model.addAttribute("categories", categoryRepository.findAll());

        String nickname = getCurrentNicknameOrNull();
        model.addAttribute("username", nickname == null ? "게스트" : nickname);

        return "post/edit";
    }

    // =========================
    // 5) 수정 처리 (POST)
    // =========================
    @PostMapping("/posts/{id}/edit")
    public String editPost(
            @PathVariable Integer id,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<MultipartFile> newImages
    ) {
        Integer userId = getCurrentUserIdOrNull();
        if (userId == null) return "redirect:/user/login";

        CategoryEntity categoryEntity = categoryRepository.findByCategoryName(category)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리: " + category));

        postService.update(id, userId, categoryEntity.getCategoryId(), title, description);

        PostEntity post = postService.get(id);
        postImageService.replaceImages(id, post, newImages);

        return "redirect:/posts/" + id;
    }

    // =========================
    // 6) 삭제 처리 (POST) - 삭제 후 "/" 로 이동
    // =========================
    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Integer id) {
        Integer userId = getCurrentUserIdOrNull();
        if (userId == null) return "redirect:/user/login";

        postService.delete(id, userId);
        return "redirect:/";
    }

    // =========================
    // 로그인 사용자 정보 헬퍼 (SecurityUtil 대체)
    // =========================
    private String getLoginIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) return null;

        String loginId = auth.getName();
        if (loginId == null || loginId.isBlank()) return null;

        return loginId;
    }

    private Integer getCurrentUserIdOrNull() {
        String loginId = getLoginIdOrNull();
        return (loginId == null) ? null
                : userRepository.findByLoginId(loginId).map(UserEntity::getUserId).orElse(null);
    }

    private String getCurrentNicknameOrNull() {
        String loginId = getLoginIdOrNull();
        return (loginId == null) ? null
                : userRepository.findByLoginId(loginId).map(UserEntity::getNickname).orElse(null);
    }
}
