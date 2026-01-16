package com.example.nasda.dto.sticker;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StickerCategoryRequestDTO {
    @NotBlank(message = "카테고리 이름은 필수입니다.")
    private String name;

    @Builder.Default
    private Boolean isActive = true;
}