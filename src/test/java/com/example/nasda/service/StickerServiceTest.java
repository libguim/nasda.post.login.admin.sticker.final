package com.example.nasda.service;

import com.example.nasda.dto.sticker.StickerCategoryRequestDTO;
import com.example.nasda.dto.sticker.StickerRequestDTO;
import com.example.nasda.dto.sticker.StickerResponseDTO;
import com.example.nasda.service.sticker.StickerCategoryService;
import com.example.nasda.service.sticker.StickerService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Log4j2
public class StickerServiceTest {

    @Autowired
    private StickerService stickerService;

    @Autowired
    private StickerCategoryService stickerCategoryService; // ★ 기초 데이터(카테고리) 생성을 위해 필요

    // 1. 스티커 등록 테스트
    @Test
    public void testRegister() {
        // [Step 1] 스티커를 붙일 '카테고리'를 먼저 하나 만듭니다. (FK 제약조건 해결)
        StickerCategoryRequestDTO categoryReq = StickerCategoryRequestDTO.builder()
                .name("Test Category for Sticker")
                .isActive(true)
                .build();

        Integer categoryId = stickerCategoryService.createCategory(categoryReq);
        log.info("준비된 카테고리 ID: " + categoryId);

        // [Step 2] 위에서 만든 카테고리 ID를 이용해 스티커 등록 요청 객체 생성
        StickerRequestDTO stickerReq = StickerRequestDTO.builder()
                .stickerCategoryId(categoryId) // ★ 생성한 카테고리 ID 연결
                .stickerName("Super Happy Dog")
                .stickerImageUrl("/test/dog.png")
                .build();

        // [Step 3] 스티커 서비스 호출
        Integer stickerId = stickerService.createSticker(stickerReq);

        // [Step 4] 검증
        log.info("생성된 스티커 ID: " + stickerId);
        Assertions.assertNotNull(stickerId);
    }

    // 2. 존재하지 않는 카테고리에 등록 시도 (예외 테스트)
    @Test
    public void testRegisterFail() {
        // 존재할 리 없는 ID 설정
        Integer wrongCategoryId = 99999;

        StickerRequestDTO stickerReq = StickerRequestDTO.builder()
                .stickerCategoryId(wrongCategoryId)
                .stickerName("Ghost Sticker")
                .stickerImageUrl("/test/ghost.png")
                .build();

        // 예외가 터져야 성공!
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stickerService.createSticker(stickerReq);
        });

        log.info("예외 발생 테스트 성공 (존재하지 않는 카테고리)");
    }

    // 3. 카테고리별 조회 테스트 (ModelMapper 동작 확인)
    @Test
    public void testGetByCategoryId() {
        // (데이터가 없으면 테스트가 안 되므로, 위 testRegister()가 실행된 후라고 가정하거나
        //  여기서 데이터를 직접 넣고 테스트하는 것이 안전합니다.)

        // 테스트를 위해 데이터 세트 준비
        StickerCategoryRequestDTO catReq = StickerCategoryRequestDTO.builder().name("Summer Vibe").build();
        Integer catId = stickerCategoryService.createCategory(catReq);

        StickerRequestDTO stickerReq = StickerRequestDTO.builder()
                .stickerCategoryId(catId)
                .stickerName("Sun")
                .stickerImageUrl("sun.png")
                .build();
        stickerService.createSticker(stickerReq);


        // [검증 시작]
        List<StickerResponseDTO> result = stickerService.getStickersByCategoryId(catId);

        log.info("조회된 스티커 개수: " + result.size());

        result.forEach(dto -> {
            log.info("----------------------------------");
            log.info("Sticker Name: " + dto.getStickerName());
            log.info("Image URL   : " + dto.getStickerImageUrl());
            // ★ 가장 중요한 확인 포인트: LOOSE 전략으로 카테고리 이름이 잘 들어왔는가?
            log.info("Category Name (Mapped): " + dto.getCategoryName());
            log.info("----------------------------------");

            // 검증 코드
            Assertions.assertEquals("Summer Vibe", dto.getCategoryName());
        });
    }

    // 4. 삭제 테스트
    @Test
    public void testDelete() {
        // 테스트용 데이터 생성
        StickerCategoryRequestDTO catReq = StickerCategoryRequestDTO.builder().name("To be deleted").build();
        Integer catId = stickerCategoryService.createCategory(catReq);

        StickerRequestDTO stickerReq = StickerRequestDTO.builder()
                .stickerCategoryId(catId)
                .stickerName("Delete Me")
                .stickerImageUrl("del.png")
                .build();
        Integer stickerId = stickerService.createSticker(stickerReq);

        log.info("삭제 전 ID: " + stickerId);

        // 삭제 수행
        stickerService.deleteSticker(stickerId);

        log.info("삭제 수행 완료");

        // (선택) 삭제 후 조회 시 에러가 나거나 비어있는지 확인할 수 있음
    }
}