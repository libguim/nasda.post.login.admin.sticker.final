package com.example.nasda.service.sticker;

import com.example.nasda.dto.sticker.StickerCategoryRequestDTO;
import com.example.nasda.dto.sticker.StickerCategoryResponseDTO;

import java.util.List;

public interface StickerCategoryService {

    // 카테고리 생성 (관리자용 기능이지만 초기 세팅에 필요)
    Integer createCategory(StickerCategoryRequestDTO requestDTO);

    // 모든 카테고리 목록 조회 (스티커 메뉴 보여줄 때 필요)
    List<StickerCategoryResponseDTO> getAllCategories();

    // 카테고리 삭제
    void deleteCategory(Integer categoryId);
}