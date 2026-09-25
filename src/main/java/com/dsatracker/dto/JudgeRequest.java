package com.dsatracker.dto;

import com.dsatracker.model.Language;

public record JudgeRequest(Language language, String code, Long contestSessionId) {
}
