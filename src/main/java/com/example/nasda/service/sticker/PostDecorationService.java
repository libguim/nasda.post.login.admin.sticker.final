package com.example.nasda.service.sticker;

import com.example.nasda.dto.sticker.PostDecorationRequestDTO;
import com.example.nasda.dto.sticker.PostDecorationResponseDTO;

import java.util.List;

public interface PostDecorationService {

    // [Create] 일괄 저장
    List<PostDecorationResponseDTO> saveDecorations(PostDecorationRequestDTO requestDTO);

    // [Read] 목록 조회
    List<PostDecorationResponseDTO> getDecorationsByImageId(Integer imageId);

    // ✅ [Update] 장식 수정 (위치, 크기, 회전 등)
    // 누가(currentUserId) 어떤 스티커(decorationId)를 어떻게(item) 바꿀 것인지 명시합니다.
    void updateDecoration(Integer decorationId, PostDecorationRequestDTO.DecorationItem updateDTO, Integer currentUserId);

    // [Delete] 삭제
    void deleteDecoration(Integer decorationId, Integer currentUserId);

    List<PostDecorationResponseDTO> getDecorationsByPostId(Integer postId);

}