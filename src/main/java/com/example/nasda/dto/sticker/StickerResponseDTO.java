package com.example.nasda.dto.sticker;

import lombok.*;

@Getter
@Setter // ModelMapper 사용 시 Setter 혹은 Field Access 필수
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StickerResponseDTO {
    private Integer stickerId;
    private String stickerName;
    private String stickerImageUrl;

    // ★ LOOSE 전략 덕분에 stickerCategory.name -> categoryName으로 자동 매핑될 예정!
    private String categoryName;
}