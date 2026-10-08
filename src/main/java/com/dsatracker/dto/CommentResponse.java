package com.dsatracker.dto;

import com.dsatracker.model.Comment;

import java.time.Instant;

public record CommentResponse(Long id, String authorName, String body, Instant createdAt, boolean mine) {
    public static CommentResponse from(Comment comment, Long currentUserId) {
        return new CommentResponse(
                comment.getId(),
                comment.getUser().getDisplayName(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUser().getId().equals(currentUserId)
        );
    }
}
