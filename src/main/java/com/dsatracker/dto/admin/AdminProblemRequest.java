package com.dsatracker.dto.admin;

import com.dsatracker.model.Difficulty;

import java.util.List;

public record AdminProblemRequest(
        Long topicId,
        String title,
        Difficulty difficulty,
        String statement,
        String timeComplexity,
        String spaceComplexity,
        List<AdminExampleDto> examples,
        List<String> constraints,
        List<String> hints,
        String editorialUrl,
        String videoUrl,
        List<String> tags,
        String externalUrl,
        int orderIndex,
        List<AdminTestCaseDto> testCases
) {
}
