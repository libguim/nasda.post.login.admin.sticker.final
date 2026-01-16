package com.example.nasda.service.sticker;

import com.example.nasda.domain.StickerCategoryEntity;
import com.example.nasda.domain.StickerEntity;
import com.example.nasda.dto.sticker.StickerRequestDTO;
import com.example.nasda.dto.sticker.StickerResponseDTO;
import com.example.nasda.repository.sticker.StickerCategoryRepository;
import com.example.nasda.repository.sticker.StickerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StickerServiceImpl implements StickerService {

    private final StickerRepository stickerRepository;
    private final StickerCategoryRepository stickerCategoryRepository;

    // RootConfig에 등록한 ModelMapper 주입
    private final ModelMapper modelMapper;

    /**
     * 스티커 등록
     * (연관 관계 설정이 필요하므로 Builder 패턴 사용)
     */
    @Override
    @Transactional
    public Integer createSticker(StickerRequestDTO requestDTO) {
        // 1. 카테고리(서가)가 실제로 존재하는지 확인
        StickerCategoryEntity category = stickerCategoryRepository.findById(requestDTO.getStickerCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. ID=" + requestDTO.getStickerCategoryId()));

        // 2. 스티커(책) 생성 및 서가 지정
        StickerEntity sticker = StickerEntity.builder()
                .stickerCategory(category) // ★ 연관 관계 매핑
                .stickerName(requestDTO.getStickerName())
                .stickerImageUrl(requestDTO.getStickerImageUrl())
                .build();

        // 3. 저장
        return stickerRepository.save(sticker).getStickerId();
    }

    /**
     * 모든 스티커 조회
     */
    @Override
    public List<StickerResponseDTO> getAllStickers() {
        return stickerRepository.findAll().stream()
                .map(entity -> modelMapper.map(entity, StickerResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 스티커 조회
     */
    @Override
    public List<StickerResponseDTO> getStickersByCategoryId(Integer categoryId) {
        // 1. Repository에서 조회 (EntityGraph 덕분에 카테고리 정보도 이미 있음)
        List<StickerEntity> stickers = stickerRepository.findByStickerCategory_StickerCategoryId(categoryId);

        // 2. ModelMapper 변환
        // LOOSE 전략 덕분에 sticker.stickerCategory.name -> dto.categoryName 자동 매핑
        return stickers.stream()
                .map(entity -> modelMapper.map(entity, StickerResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * 스티커 삭제
     */
    @Override
    @Transactional
    public void deleteSticker(Integer stickerId) {
        if (!stickerRepository.existsById(stickerId)) {
            throw new IllegalArgumentException("삭제할 스티커가 없습니다. ID=" + stickerId);
        }
        stickerRepository.deleteById(stickerId);
    }
}