package com.dsatracker.dto;

import com.dsatracker.model.Sheet;

public record SheetResponse(Long id, String slug, String name, String description, int orderIndex) {
    public static SheetResponse from(Sheet s) {
        return new SheetResponse(s.getId(), s.getSlug(), s.getName(), s.getDescription(), s.getOrderIndex());
    }
}
