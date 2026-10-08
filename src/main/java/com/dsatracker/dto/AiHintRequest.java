package com.dsatracker.dto;

/** Both fields optional -- a hint can be requested before any code has been written. */
public record AiHintRequest(String code, String language) {
}
