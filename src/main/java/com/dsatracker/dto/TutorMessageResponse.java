package com.dsatracker.dto;

import com.dsatracker.model.ChatRole;

import java.time.Instant;

public record TutorMessageResponse(Long id, ChatRole role, String content, Instant createdAt) {
}
