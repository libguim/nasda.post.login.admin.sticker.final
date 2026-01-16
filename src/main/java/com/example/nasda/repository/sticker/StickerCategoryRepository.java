package com.example.nasda.repository.sticker;

import com.example.nasda.domain.StickerCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StickerCategoryRepository extends JpaRepository<StickerCategoryEntity, Integer> {
    // 카테고리 이름으로 중복 확인이나 조회를 하기 위해 추가하면 좋은 메서드
    Optional<StickerCategoryEntity> findByName(String name);
}