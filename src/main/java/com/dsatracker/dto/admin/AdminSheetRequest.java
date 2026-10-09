package com.dsatracker.dto.admin;

public record AdminSheetRequest(String slug, String name, String description, int orderIndex) {
}
