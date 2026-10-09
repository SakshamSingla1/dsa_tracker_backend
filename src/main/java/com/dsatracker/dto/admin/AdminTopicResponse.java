package com.dsatracker.dto.admin;

import com.dsatracker.model.Topic;

public record AdminTopicResponse(Long id, String name, int orderIndex, Long sheetId, String sheetName, int problemCount) {
    public static AdminTopicResponse from(Topic t) {
        return new AdminTopicResponse(
                t.getId(), t.getName(), t.getOrderIndex(),
                t.getSheet() != null ? t.getSheet().getId() : null,
                t.getSheet() != null ? t.getSheet().getName() : null,
                t.getProblems().size()
        );
    }
}
