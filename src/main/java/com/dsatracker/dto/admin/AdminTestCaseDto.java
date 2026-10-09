package com.dsatracker.dto.admin;

import com.dsatracker.model.TestCase;

public record AdminTestCaseDto(Long id, String input, String expectedOutput, boolean sample) {
    public static AdminTestCaseDto from(TestCase t) {
        return new AdminTestCaseDto(t.getId(), t.getInput(), t.getExpectedOutput(), t.isSample());
    }
}
