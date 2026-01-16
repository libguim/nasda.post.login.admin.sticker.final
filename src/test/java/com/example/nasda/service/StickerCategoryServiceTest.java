package com.example.nasda.service;

import com.example.nasda.dto.sticker.StickerCategoryRequestDTO;
import com.example.nasda.dto.sticker.StickerCategoryResponseDTO;
import com.example.nasda.service.sticker.StickerCategoryService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Log4j2
public class StickerCategoryServiceTest {

    @Autowired
    private StickerCategoryService stickerCategoryService;

    // 1. 카테고리 생성 (등록) 테스트
    @Test
    public void testRegister() {
        // Given: 등록할 DTO 생성
        StickerCategoryRequestDTO requestDTO = StickerCategoryRequestDTO.builder()
                .name("Integration Test Category")
                .isActive(true)
                .build();

        // When: 실제 서비스 메서드 호출 (DB에 저장됨)
        Integer createdId = stickerCategoryService.createCategory(requestDTO);

        // Then: 결과 확인
        log.info("생성된 카테고리 ID: " + createdId);
        Assertions.assertNotNull(createdId);
    }

    // 2. 중복 예외 발생 테스트 (비즈니스 로직 검증)
    @Test
    public void testRegisterDuplicate() {
        // Given: 이미 존재하는 이름으로 DTO 생성
        // (testRegister가 먼저 실행되어 데이터가 있을 수도 있고, 없을 수도 있으니 먼저 하나 저장)
        String duplicateName = "Duplicate Check";

        StickerCategoryRequestDTO dto1 = StickerCategoryRequestDTO.builder()
                .name(duplicateName).build();
        stickerCategoryService.createCategory(dto1); // 1차 저장 성공

        // When & Then: 똑같은 이름으로 다시 저장 시도 -> 예외 발생해야 함
        StickerCategoryRequestDTO dto2 = StickerCategoryRequestDTO.builder()
                .name(duplicateName).build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stickerCategoryService.createCategory(dto2);
        });

        log.info("중복 예외 테스트 통과 (IllegalArgumentException 발생함)");
    }

    // 3. 전체 목록 조회 테스트
    @Test
    public void testGetAll() {
        // When: 목록 조회
        List<StickerCategoryResponseDTO> result = stickerCategoryService.getAllCategories();

        // Then: 로그 출력
        log.info("총 카테고리 수: " + result.size());

        result.forEach(dto -> {
            log.info("--------------------------------");
            log.info("DTO: " + dto); // DTO의 toString() 혹은 객체 정보 출력
            log.info("Name: " + dto.getName());
            log.info("--------------------------------");
        });
    }

    // 4. 삭제 테스트
    @Test
    public void testDelete() {
        // 테스트를 위해 데이터를 하나 만듭니다.
        StickerCategoryRequestDTO dto = StickerCategoryRequestDTO.builder()
                .name("Delete Me")
                .build();
        Integer id = stickerCategoryService.createCategory(dto);

        log.info("삭제 전 생성된 ID: " + id);

        // 삭제 실행
        stickerCategoryService.deleteCategory(id);

        log.info("삭제 완료");

        // (선택 사항) 삭제 후 조회가 안 되는지 검증하는 로직을 추가할 수도 있습니다.
    }
}