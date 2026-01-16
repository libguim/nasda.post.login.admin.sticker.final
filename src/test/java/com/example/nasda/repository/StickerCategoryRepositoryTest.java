package com.example.nasda.repository;

import com.example.nasda.domain.StickerCategoryEntity;
import com.example.nasda.repository.sticker.StickerCategoryRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.stream.IntStream;

@SpringBootTest
@Log4j2
public class StickerCategoryRepositoryTest {

    @Autowired
    private StickerCategoryRepository stickerCategoryRepository;

    @Test
    public void testInsert() {
        // 1번부터 10번까지 반복하며 카테고리 더미 데이터 생성
        IntStream.rangeClosed(1, 10).forEach(i -> {
            StickerCategoryEntity category = StickerCategoryEntity.builder()
                    .name("Category..." + i)
                    .isActive(true)
                    .build();

            StickerCategoryEntity result = stickerCategoryRepository.save(category);
            log.info("CATEGORY ID: " + result.getStickerCategoryId());
        });
    }

    @Test
    public void testSelect() {
        // 테스트용 ID (DB에 실제 존재하는 ID여야 함. testInsert 실행 후 확인 필요)
        Integer id = 5;

        Optional<StickerCategoryEntity> result = stickerCategoryRepository.findById(id);

        StickerCategoryEntity category = result.orElseThrow();
        log.info(category);
    }

    @Test
    public void testUpdate() {
        Integer id = 3;

        Optional<StickerCategoryEntity> result = stickerCategoryRepository.findById(id);
        StickerCategoryEntity category = result.orElseThrow();

        // 엔티티에 수정 메서드(changeName 등)가 있다고 가정하거나,
        // @Setter가 있다면 아래와 같이 변경 (여기서는 Builder 패턴의 toBuilder나 Setter 가정)
        // 만약 Entity에 update 메서드가 없다면 추가해야 합니다.

        // 예시: 엔티티에 update 메서드를 만들었다고 가정
        // category.change("Updated Category 5");

        // 혹은 다시 빌더로 만들어서 저장 (불변 객체 스타일)
        StickerCategoryEntity updatedCategory = StickerCategoryEntity.builder()
                .stickerCategoryId(category.getStickerCategoryId()) // ID 유지
                .name("Updated Name..." + id)
                .isActive(category.getIsActive())
                .build();

        stickerCategoryRepository.save(updatedCategory);
        log.info("UPDATED: " + updatedCategory);
    }

    @Test
    public void testDelete() {
        Integer id = 5;
        stickerCategoryRepository.deleteById(id);
        log.info("DELETED ID: " + id);
    }

    // 참고: testSearch, testPaging 등은 Querydsl 설정이 추가된 후에 작성할 수 있습니다.
    // 현재는 기본적인 CRUD 구조만 맞췄습니다.
}