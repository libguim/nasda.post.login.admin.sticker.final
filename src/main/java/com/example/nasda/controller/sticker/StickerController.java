package com.example.nasda.controller.sticker;

import com.example.nasda.dto.sticker.StickerRequestDTO;
import com.example.nasda.dto.sticker.StickerResponseDTO;
import com.example.nasda.service.sticker.StickerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/api/stickers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StickerController {

    private final StickerService stickerService;

    /**
     * 1. 스티커 등록
     * [Postman 설정]
     * - Method: POST
     * - URL: http://localhost:8080/api/stickers
     * - Body (Raw/JSON):
     * {
     * "stickerCategoryId": 1,
     * "stickerName": "Smiling Sun",
     * "stickerImageUrl": "/images/sun.png"
     * }
     */
    @PostMapping
    public ResponseEntity<Integer> createSticker(@RequestBody StickerRequestDTO requestDTO) {
        log.info("✨ [스티커 등록] 카테고리ID={}, 이름={}",
                requestDTO.getStickerCategoryId(), requestDTO.getStickerName());

        Integer stickerId = stickerService.createSticker(requestDTO);

        log.info("✅ [등록 완료] 스티커 ID: {}", stickerId);
        return ResponseEntity.ok(stickerId);
    }

    /**
     * 2. 특정 카테고리의 스티커 목록 조회
     * [Postman 설정]
     * - Method: GET
     * - URL: http://localhost:8080/api/stickers/categories/1
     */
    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<List<StickerResponseDTO>> getStickersByCategory(@PathVariable Integer categoryId) {
        log.info("📂 [카테고리별 조회] 카테고리 ID={} 의 스티커를 찾습니다.", categoryId);

        List<StickerResponseDTO> stickers = stickerService.getStickersByCategoryId(categoryId);

        return ResponseEntity.ok(stickers);
    }

    /**
     * 3. 스티커 삭제
     * [Postman 설정]
     * - Method: DELETE
     * - URL: http://localhost:8080/api/stickers/5
     */
    @DeleteMapping("/{stickerId}")
    public ResponseEntity<String> deleteSticker(@PathVariable Integer stickerId) {
        log.info("🗑️ [스티커 삭제] ID: {}", stickerId);
        stickerService.deleteSticker(stickerId);
        return ResponseEntity.ok("삭제 성공");
    }
}