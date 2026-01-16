package com.example.nasda.service.sticker;

import com.example.nasda.domain.StickerCategoryEntity;
import com.example.nasda.dto.sticker.StickerCategoryRequestDTO;
import com.example.nasda.dto.sticker.StickerCategoryResponseDTO;
import com.example.nasda.repository.sticker.StickerCategoryRepository;
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
public class StickerCategoryServiceImpl implements StickerCategoryService {

    private final StickerCategoryRepository stickerCategoryRepository;
    private final ModelMapper modelMapper;

    /**
     * 카테고리 생성
     */
    @Override
    @Transactional
    public Integer createCategory(StickerCategoryRequestDTO requestDTO) {
        // 1. 중복 검사 (Repository에 추가하신 findByName 활용!)
        if (stickerCategoryRepository.findByName(requestDTO.getName()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 카테고리 이름입니다: " + requestDTO.getName());
        }

        // 2. Entity 조립 (저장은 역시 Builder가 명확합니다)
        StickerCategoryEntity category = StickerCategoryEntity.builder()
                .name(requestDTO.getName())
                .isActive(requestDTO.getIsActive())
                .build();

        // 3. 저장 및 ID 반환
        return stickerCategoryRepository.save(category).getStickerCategoryId();
    }

    /**
     * 전체 카테고리 조회
     * (스티커 메뉴바에 "감정", "사물", "동물" 등을 띄워줄 때 사용)
     */
    @Override
    public List<StickerCategoryResponseDTO> getAllCategories() {
        // 1. 전체 조회
        List<StickerCategoryEntity> categories = stickerCategoryRepository.findAll();

        // 2. 변환 (ModelMapper의 힘을 빌려 한 줄로 처리!)
        // Entity와 DTO의 필드명(name, isActive)이 똑같아서 아주 매끄럽게 변환됩니다.
        return categories.stream()
                .map(entity -> modelMapper.map(entity, StickerCategoryResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 삭제
     */
    @Override
    @Transactional
    public void deleteCategory(Integer categoryId) {
        // 실제 존재하는지 확인 후 삭제
        StickerCategoryEntity category = stickerCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 카테고리가 없습니다. ID=" + categoryId));

        stickerCategoryRepository.delete(category);
    }
}