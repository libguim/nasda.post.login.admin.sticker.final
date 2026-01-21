package com.example.nasda.dto.comment;

import java.util.List;

public record CommentPageResponse(
        List<CommentItem> comments,
        PageInfo page
) {
    public record CommentItem(
            Integer id,
            String content,
            String authorNickname,
            String createdAtText,
            boolean canEdit
    ) {}

    public record PageInfo(
            int number,
            int size,
            int totalPages,
            long totalElements,
            boolean hasPrevious,
            boolean hasNext
    ) {}
}
