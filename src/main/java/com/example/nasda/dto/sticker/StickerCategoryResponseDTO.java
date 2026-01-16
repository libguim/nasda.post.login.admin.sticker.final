package com.example.nasda.dto.sticker;

import lombok.*;

@Getter
@Setter // ModelMapper가 값을 넣으려면 Setter 혹은 Field Access 설정 필요
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StickerCategoryResponseDTO {
    private Integer stickerCategoryId;
    private String name;
    private Boolean isActive;
}