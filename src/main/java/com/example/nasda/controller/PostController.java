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
    // 1. 글 작성 페이지 (GET)
    // =========================
    // 중요: {postId} 상세 보기보다 무조건 위에 있어야 400 에러가 나지 않습니다.
    @GetMapping("/posts/create")
    public String createForm(Model model) {
        model.addAttribute("postCreateRequestDto", new PostCreateRequestDto("", "", ""));
        model.addAttribute("categories", categoryRepository.findAll());

        // 헤더용 사용자 닉네임
        String nickname = getCurrentNicknameOrNull();
        model.addAttribute("username", nickname == null ? "게스트" : nickname);

        return "post/create";
    }

    // =========================
    // 2. 게시글 상세 보기 (ID 변수 처리)
    // =========================

    // [수정] String으로 받아 "create" 같은 문자가 들어왔을 때 400 에러가 나지 않도록 방어합니다.
    @GetMapping("/posts/{postId}")
    public String viewCompat(@PathVariable("postId") String postIdStr) {
        try {
            // 숫자인 경우에만 상세 페이지로 리다이렉트
            Integer postId = Integer.parseInt(postIdStr);
            return "redirect:/post/view.html?id=" + postId;
        } catch (NumberFormatException e) {
            // "create"나 "write" 같은 글자가 들어오면 글쓰기 페이지로 보냅니다.
            return "redirect:/posts/create";
        }
    }

    // 실제 상세 페이지 렌더링
    @GetMapping("/post/view.html")
    public String viewPost(
            @RequestParam("id") Integer postId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model
    ) {
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
    }

    // =========================
    // 3. 홈 리스트 및 글 등록(POST)
    // =========================
    @GetMapping("/posts")
    public String list(Model model) {
        model.addAttribute("posts", postService.getHomePosts());
        return "index";
    }

    @PostMapping("/posts")
    public String createPost(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<org.springframework.web.multipart.MultipartFile> images
    ) {
        Integer userId = getCurrentUserIdOrNull();
        if (userId == null) return "redirect:/user/login";

        CategoryEntity categoryEntity = categoryRepository.findByCategoryName(category)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리: " + category));

        PostEntity post = postService.create(userId, categoryEntity.getCategoryId(), title, description);
        postImageService.addImages(post, images);

        return "redirect:/post/view.html?id=" + post.getPostId();
    }

    // ... (editForm, editPost, deletePost 및 사용자 정보 메서드들 생략 - 기존과 동일) ...

    private String getLoginIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) return null;
        return auth.getName();
    }

    private Integer getCurrentUserIdOrNull() {
        String loginId = getLoginIdOrNull();
        return (loginId == null) ? null : userRepository.findByLoginId(loginId).map(UserEntity::getUserId).orElse(null);
    }

    private String getCurrentNicknameOrNull() {
        String loginId = getLoginIdOrNull();
        return (loginId == null) ? null : userRepository.findByLoginId(loginId).map(UserEntity::getNickname).orElse(null);
    }
}