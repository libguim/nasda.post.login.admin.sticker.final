package com.example.nasda.controller.sticker;

import com.example.nasda.dto.sticker.StickerCategoryRequestDTO;
import com.example.nasda.dto.sticker.StickerCategoryResponseDTO;
import com.example.nasda.service.sticker.StickerCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/api/sticker-categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Postman 테스트 할 때도 켜두는 게 속 편합니다
public class StickerCategoryController {

    private final StickerCategoryService stickerCategoryService;

    // 1. 카테고리 생성
    @PostMapping
    public ResponseEntity<Integer> createCategory(@RequestBody StickerCategoryRequestDTO requestDTO) {
        log.info("📂 [카테고리 생성] 이름: {}", requestDTO.getName());
        Integer createdId = stickerCategoryService.createCategory(requestDTO);
        return ResponseEntity.ok(createdId);
    }

    // 2. 전체 카테고리 조회
    @GetMapping
    public ResponseEntity<List<StickerCategoryResponseDTO>> getAllCategories() {
        log.info("🔍 [카테고리 목록 조회]");
        return ResponseEntity.ok(stickerCategoryService.getAllCategories());
    }

    // 3. 카테고리 삭제
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<String> deleteCategory(@PathVariable Integer categoryId) {
        log.info("🗑️ [카테고리 삭제] ID: {}", categoryId);
        stickerCategoryService.deleteCategory(categoryId);
        return ResponseEntity.ok("삭제 성공");
    }
}