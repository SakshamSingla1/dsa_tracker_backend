package com.dsatracker.dto;

/** `code`/`language` optional context from the editor, so the tutor can see what the user has so far. */
public record TutorMessageRequest(String content, String code, String language) {
}
