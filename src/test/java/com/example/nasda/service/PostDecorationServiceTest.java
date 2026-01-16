package com.example.nasda.service;

import com.example.nasda.dto.sticker.PostDecorationRequestDTO;
import com.example.nasda.dto.sticker.PostDecorationResponseDTO;
import com.example.nasda.service.sticker.PostDecorationService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Log4j2
public class PostDecorationServiceTest {

    @Autowired
    private PostDecorationService postDecorationService;

    @Test
    @DisplayName("스티커 일괄 등록 및 도배 방지 테스트")
    public void testRegisterMultiple() {
        log.info("--------- 스티커 일괄 등록 테스트 ---------");

        List<PostDecorationRequestDTO.DecorationItem> items = new ArrayList<>();

        // 실제 DB에 존재하는 Sticker ID를 사용하세요 (HeidiSQL 스크린샷 기준 1, 2번 사용 가능)
        items.add(PostDecorationRequestDTO.DecorationItem.builder()
                .stickerId(1).posX(10.0f).posY(20.0f).scale(1.0f).build());
        items.add(PostDecorationRequestDTO.DecorationItem.builder()
                .stickerId(2).posX(30.0f).posY(40.0f).scale(1.0f).build());

        PostDecorationRequestDTO requestDTO = PostDecorationRequestDTO.builder()
                .postImageId(12) // HeidiSQL 기준 존재하는 이미지 ID
                .userId(1)      // 존재하는 유저 ID
                .decorations(items)
                .build();

        try {
            List<PostDecorationResponseDTO> resultList = postDecorationService.saveDecorations(requestDTO);
            log.info("성공적으로 저장된 스티커 개수: " + resultList.size());
        } catch (IllegalStateException e) {
            log.warn("도배 방지 로직 작동: " + e.getMessage());
        } catch (Exception e) {
            log.error("저장 실패: " + e.getMessage());
            throw e;
        }
    }

    @Test
    @DisplayName("이미지별 장식 목록 조회 테스트")
    public void testGetListByImage() {
        log.info("--------- 이미지별 장식 목록 조회 테스트 ---------");
        Integer imageId = 12; // 테스트할 이미지 ID

        List<PostDecorationResponseDTO> list = postDecorationService.getDecorationsByImageId(imageId);

        log.info("조회된 장식 개수: " + list.size());
        list.forEach(dto -> log.info("장식 ID: {}, 스티커 ID: {}", dto.getDecorationId(), dto.getStickerId()));
    }

    @Test
    @DisplayName("게시글 전체 장식 목록 조회 테스트 (Post ID 기준)")
    public void testGetListByPost() {
        log.info("--------- 게시글 전체 장식 목록 조회 테스트 ---------");
        Integer postId = 10; // 테스트할 게시글 ID (실제 DB에 존재하는 ID)

        List<PostDecorationResponseDTO> list = postDecorationService.getDecorationsByPostId(postId);

        log.info("조회된 총 장식 개수: " + list.size());
        // 각 스티커가 어떤 이미지(postImageId)에 속해 있는지 확인
        list.forEach(dto -> log.info("이미지 ID: {}, 장식 ID: {}, URL: {}",
                dto.getPostImageId(), dto.getDecorationId(), dto.getStickerImageUrl()));
    }

    @Test
    @DisplayName("장식 위치 및 속성 수정 테스트 (Update)")
    public void testUpdateDecoration() {
        log.info("--------- 장식 수정 테스트 (권한 체크 포함) ---------");

        // 1. 수정할 장식 ID와 요청자 ID 설정
        Integer decorationId = 1; // DB에 실존하는 장식 ID
        Integer currentUserId = 1; // 수정을 시도하는 유저 (장식을 붙인 본인)

        // 2. 수정할 데이터 준비 (DTO 내부의 DecorationItem 활용)
        PostDecorationRequestDTO.DecorationItem updateDTO = PostDecorationRequestDTO.DecorationItem.builder()
                .posX(500.0f)   // 새로운 X 좌표
                .posY(300.0f)   // 새로운 Y 좌표
                .scale(1.5f)    // 크기 변경
                .rotation(45.0f)// 회전 각도 변경
                .build();

        try {
            // 3. 서비스 호출
            postDecorationService.updateDecoration(decorationId, updateDTO, currentUserId);
            log.info(decorationId + "번 장식 수정 성공!");

            // 4. 검증 (선택 사항: 다시 조회해서 값이 바뀌었는지 확인)
            List<PostDecorationResponseDTO> list = postDecorationService.getDecorationsByImageId(12);
            list.stream()
                    .filter(dto -> dto.getDecorationId().equals(decorationId))
                    .forEach(dto -> log.info("수정 후 위치 확인 -> X: {}, Y: {}", dto.getPosX(), dto.getPosY()));

        } catch (SecurityException e) {
            log.error("수정 실패: 권한이 없습니다. (" + e.getMessage() + ")");
        } catch (Exception e) {
            log.error("수정 실패 사유: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("권한 기반 장식 삭제 테스트")
    public void testRemove() {
        log.info("--------- 장식 삭제 테스트 (권한 체크 포함) ---------");

        // 1. 삭제할 장식 ID와 요청자 ID 설정
        Integer decorationId = 5; // DB에 실존하는 장식 ID
        Integer currentUserId = 1; // 삭제를 시도하는 유저 (장식 본인 혹은 사진 주인)

        try {
            // ✅ 수정된 인터페이스에 따라 currentUserId를 함께 전달합니다.
            postDecorationService.deleteDecoration(decorationId, currentUserId);
            log.info(decorationId + "번 장식 삭제 성공 (요청자 ID: " + currentUserId + ")");

        } catch (SecurityException e) {
            log.error("삭제 실패: 권한이 없습니다. (" + e.getMessage() + ")");
        } catch (Exception e) {
            log.error("삭제 실패 사유: " + e.getMessage());
        }
    }
}