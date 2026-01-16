package com.example.nasda.controller.sticker;

import com.example.nasda.dto.sticker.PostDecorationRequestDTO;
import com.example.nasda.dto.sticker.PostDecorationResponseDTO;
import com.example.nasda.service.sticker.PostDecorationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/api/decorations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PostDecorationController {

    private final PostDecorationService postDecorationService;

    /**
     * 1. 스티커 일괄 저장
     */
    @PostMapping("")
    public ResponseEntity<List<PostDecorationResponseDTO>> saveDecorations(@RequestBody PostDecorationRequestDTO requestDTO) {
        log.info("✨ [꾸미기 저장] 게시글 이미지(ID={}) 위에 {}개의 스티커 부착 요청",
                requestDTO.getPostImageId(),
                requestDTO.getDecorations() != null ? requestDTO.getDecorations().size() : 0);

        List<PostDecorationResponseDTO> savedDecorations = postDecorationService.saveDecorations(requestDTO);

        log.info("✅ [저장 완료] 총 {}개의 장식 저장됨", savedDecorations.size());
        return ResponseEntity.ok(savedDecorations);
    }

    /**
     * 2. 꾸미기 조회 (이미지별)
     */
    @GetMapping("/image/{imageId}")
    public List<PostDecorationResponseDTO> getDecorations(@PathVariable Integer imageId) {
        log.debug("🔍 [꾸미기 조회] 이미지 ID={} 에 부착된 스티커 목록 조회", imageId);

        List<PostDecorationResponseDTO> decorations = postDecorationService.getDecorationsByImageId(imageId);

        return decorations;
    }

    /**
     * 3. 스티커 위치/속성 수정 (Update)
     * 사용자가 드래그 앤 드롭으로 스티커를 옮기거나 크기를 변경했을 때 호출합니다.
     */
    @PutMapping("/{decorationId}")
    public ResponseEntity<String> updateDecoration(
            @PathVariable Integer decorationId,
            @RequestBody PostDecorationRequestDTO.DecorationItem updateDTO, // 수정할 좌표/스케일 정보
            @RequestParam Integer currentUserId // 수정 권한 확인을 위한 유저 ID
    ) {
        log.info("🔄 [꾸미기 수정] 장식 ID={} 수정 요청 (요청자: {})", decorationId, currentUserId);

        postDecorationService.updateDecoration(decorationId, updateDTO, currentUserId);

        return ResponseEntity.ok("성공적으로 수정되었습니다.");
    }

    /**
     * 4. 스티커 떼기 (삭제 권한 체크 포함)
     * [변경점] 삭제를 요청하는 사용자의 ID를 함께 전달받아야 합니다.
     */
    @DeleteMapping("/{decorationId}")
    public ResponseEntity<String> deleteDecoration(
            @PathVariable Integer decorationId,
            @RequestParam Integer currentUserId // ✅ 클라이언트로부터 현재 로그인한 유저 ID를 받음
    ) {
        log.info("🗑️ [꾸미기 삭제] 장식 ID={} 삭제 요청 (요청자: {})", decorationId, currentUserId);

        // 변경된 서비스 인터페이스에 따라 두 개의 인자를 전달합니다.
        postDecorationService.deleteDecoration(decorationId, currentUserId);

        return ResponseEntity.ok("성공적으로 삭제되었습니다.");
    }

    /**
     * 5. 게시글 전체 꾸미기 조회 (Post ID 기준)
     * 페이지 로드시 해당 게시글의 모든 이미지에 붙은 스티커를 한꺼번에 가져옵니다.
     */
    @GetMapping("/post/{postId}")
    public List<PostDecorationResponseDTO> getDecorationsByPostId(@PathVariable Integer postId) {
        log.debug("🔍 [게시글 전체 조회] 게시글 ID={} 에 부착된 모든 스티커 조회", postId);
        // 서비스에도 이 메서드를 구현해야 합니다.
        return postDecorationService.getDecorationsByPostId(postId);
    }

}