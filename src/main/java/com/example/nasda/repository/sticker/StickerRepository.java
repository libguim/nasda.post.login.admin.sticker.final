package com.example.nasda.repository.sticker;

import com.example.nasda.domain.StickerEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StickerRepository extends JpaRepository<StickerEntity, Integer> {

    // [추가] 특정 카테고리 ID를 가진 스티커들만 조회
    // @EntityGraph를 써서 카테고리 정보(stickerCategory)까지 한 번에 가져오면 성능이 더 좋습니다.
    // (ModelMapper가 카테고리 이름을 꺼낼 때 쿼리가 추가로 나가는 것을 방지함)
    @EntityGraph(attributePaths = {"stickerCategory"})
    List<StickerEntity> findByStickerCategory_StickerCategoryId(Integer categoryId);
}