package com.example.nasda.service.sticker;

import com.example.nasda.dto.sticker.StickerRequestDTO;
import com.example.nasda.dto.sticker.StickerResponseDTO;

import java.util.List;

public interface StickerService {
    // 스티커 등록
    Integer createSticker(StickerRequestDTO requestDTO);

    // 전체 스티커 조회
    List<StickerResponseDTO> getAllStickers();

    // 카테고리별 스티커 조회 (예: "감정" 스티커만 보기)
    List<StickerResponseDTO> getStickersByCategoryId(Integer categoryId);

    // 스티커 삭제
    void deleteSticker(Integer stickerId);
}