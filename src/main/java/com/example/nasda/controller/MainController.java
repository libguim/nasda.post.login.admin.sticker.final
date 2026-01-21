package com.example.nasda.controller;

import com.example.nasda.dto.post.HomePostDto;
import com.example.nasda.service.AuthUserService;
import com.example.nasda.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final PostService postService;
    private final AuthUserService authUserService;

    @GetMapping("/")
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            Model model
    ) {
        String q = (keyword == null) ? "" : keyword.trim();
        String t = (type == null || type.isBlank()) ? "content" : type.trim();
        boolean isSearch = !q.isEmpty();

        model.addAttribute("isSearchPage", isSearch);
        model.addAttribute("keyword", q);
        model.addAttribute("type", t);
        model.addAttribute("category", (category == null || category.isBlank()) ? "전체" : category);

        if (isSearch) {
            List<HomePostDto> results = postService.searchHomePosts(q, t);

            model.addAttribute("posts", results);

            // 검색 결과는 "일단" 무한스크롤 끔
            model.addAttribute("hasNext", false);
            model.addAttribute("nextPage", 0);
            model.addAttribute("size", size);
        } else {
            Pageable pageable = PageRequest.of(page, size);
            Page<HomePostDto> postsPage = postService.getHomePostsByCategory(category, pageable);

            model.addAttribute("posts", postsPage.getContent());
            model.addAttribute("hasNext", postsPage.hasNext());
            model.addAttribute("nextPage", postsPage.getNumber() + 1);
            model.addAttribute("size", size);
        }

        String nickname = authUserService.getCurrentNicknameOrNull();
        model.addAttribute("username", nickname == null ? "게스트" : nickname);

        return "index";
    }

    @GetMapping("/api/posts")
    @ResponseBody
    public Page<HomePostDto> apiPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return postService.getHomePostsByCategory(category, pageable);
    }
}
