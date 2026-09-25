package com.dsatracker.dto;

import com.dsatracker.model.Language;

public record RunRequest(Language language, String code, String stdin) {
}
