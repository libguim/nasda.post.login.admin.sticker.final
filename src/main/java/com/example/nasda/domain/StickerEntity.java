package com.example.nasda.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stickers")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class StickerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer stickerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sticker_category_id", nullable = false)
    private StickerCategoryEntity stickerCategory;

    private String stickerName;
    private String stickerImageUrl;

    @CreationTimestamp
    private LocalDateTime createdAt;
}