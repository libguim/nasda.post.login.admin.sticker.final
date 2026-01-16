package com.example.nasda.repository;

import com.example.nasda.domain.StickerCategoryEntity;
import com.example.nasda.domain.StickerEntity;
import com.example.nasda.repository.sticker.StickerCategoryRepository;
import com.example.nasda.repository.sticker.StickerRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.IntStream;

@SpringBootTest
@Log4j2
public class StickerRepositoryTest {

    @Autowired
    private StickerRepository stickerRepository;

    @Autowired
    private StickerCategoryRepository stickerCategoryRepository; // ★ 중요: 부모 엔티티 처리를 위해 필요

    @Test
    public void testInsert() {
        // 1. 기초 공사: 스티커를 붙일 카테고리를 먼저 하나 만듭니다.
        StickerCategoryEntity category = StickerCategoryEntity.builder()
                .name("Emotions")
                .isActive(true)
                .build();

        stickerCategoryRepository.save(category); // DB에 카테고리 저장 (ID 생성됨)

        // 2. 스티커 10개 생성 (IntStream 사용)
        IntStream.rangeClosed(1, 10).forEach(i -> {
            StickerEntity sticker = StickerEntity.builder()
                    .stickerCategory(category) // 위에서 만든 카테고리 연결
                    .stickerName("Emotion Sticker..." + i)
                    .stickerImageUrl("/stickers/emotion_" + i + ".png")
                    .build();

            StickerEntity result = stickerRepository.save(sticker);
            log.info("STICKER ID: " + result.getStickerId());
        });
    }

    @Test
    @Transactional
    public void testSelect() {
        // 테스트할 ID (Insert 테스트가 선행되어 데이터가 있다고 가정)
        Integer stickerId = 5;

        Optional<StickerEntity> result = stickerRepository.findById(stickerId);

        StickerEntity sticker = result.orElseThrow();

        log.info("------------------------------------------------");
        log.info("Sticker: " + sticker);
        log.info("Category Name: " + sticker.getStickerCategory().getName()); // 연관 데이터 확인
        log.info("------------------------------------------------");
    }

    @Test
    public void testUpdate() {
        Integer stickerId = 5;

        // 1. 조회
        Optional<StickerEntity> result = stickerRepository.findById(stickerId);
        StickerEntity sticker = result.orElseThrow();

        // 2. 수정 (Setter가 없으므로 Builder로 새로운 객체 생성하여 덮어쓰기)
        // (만약 Entity에 change 메서드가 있다면 그걸 쓰시면 됩니다)
        StickerEntity updatedSticker = StickerEntity.builder()
                .stickerId(sticker.getStickerId()) // ID 유지 (핵심)
                .stickerCategory(sticker.getStickerCategory()) // 카테고리 유지
                .stickerName("Updated Name..." + stickerId) // 이름 변경
                .stickerImageUrl("/stickers/updated_" + stickerId + ".png") // 이미지 변경
                .build();

        // 3. 저장 (ID가 있으므로 update 쿼리가 나갑니다)
        stickerRepository.save(updatedSticker);

        log.info("UPDATED STICKER: " + updatedSticker);
    }

    @Test
    public void testDelete() {
        Integer stickerId = 10;

        stickerRepository.deleteById(stickerId);

        log.info("DELETED STICKER ID: " + stickerId);
    }

    // 추후 Querydsl 설정 후 testSearch 등을 추가할 수 있습니다.
}