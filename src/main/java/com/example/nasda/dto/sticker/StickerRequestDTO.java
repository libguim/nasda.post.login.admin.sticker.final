package com.example.nasda.dto.sticker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StickerRequestDTO {

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Integer stickerCategoryId;

    @NotBlank(message = "스티커 이름은 필수입니다.")
    private String stickerName;

    @NotBlank(message = "스티커 이미지 경로는 필수입니다.")
    private String stickerImageUrl;

}