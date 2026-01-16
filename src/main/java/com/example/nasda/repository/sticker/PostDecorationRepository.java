package com.example.nasda.repository.sticker;

import com.example.nasda.domain.PostDecorationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PostDecorationRepository extends JpaRepository<PostDecorationEntity, Integer> {

    // ✅ 도배 방지: 한 유저가 한 이미지에 몇 개나 붙였는지 확인
    long countByUser_UserIdAndPostImage_ImageId(Integer userId, Integer imageId);

    // ✅ 목록 조회: 이미지별 스티커 리스트 (성능 최적화 포함)
    @EntityGraph(attributePaths = {"sticker"})
    List<PostDecorationEntity> findByPostImage_ImageId(Integer imageId);

    // ✅ 연쇄 청소: 이미지나 게시글 삭제 시 깔끔하게 스티커 제거
    @Modifying
    @Transactional
    void deleteByPostPostId(Integer postId);

    @Modifying
    @Transactional
    void deleteByPostImageImageId(Integer imageId);

    // ✅ 검증용 조회: 메서드명에서 언더바(_) 제거
    List<PostDecorationEntity> findByPostPostId(Integer postId);

    @Modifying
    @Transactional
    @Query("UPDATE PostDecorationEntity d SET d.posX = :x, d.posY = :y WHERE d.decorationId = :id")
    void updatePosition(@Param("id") Integer id, @Param("x") Float x, @Param("y") Float y);


}