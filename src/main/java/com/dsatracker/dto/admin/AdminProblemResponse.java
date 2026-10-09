package com.dsatracker.dto.admin;

import com.dsatracker.model.Difficulty;
import com.dsatracker.model.Problem;

import java.util.List;

public record AdminProblemResponse(
        Long id,
        Long topicId,
        String topicName,
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
    public static AdminProblemResponse from(Problem p) {
        return new AdminProblemResponse(
                p.getId(),
                p.getTopic() != null ? p.getTopic().getId() : null,
                p.getTopic() != null ? p.getTopic().getName() : null,
                p.getTitle(),
                p.getDifficulty(),
                p.getStatement(),
                p.getTimeComplexity(),
                p.getSpaceComplexity(),
                p.getExamples().stream().map(AdminExampleDto::from).toList(),
                p.getConstraints().stream().toList(),
                p.getHints().stream().toList(),
                p.getEditorialUrl(),
                p.getVideoUrl(),
                p.getTags().stream().toList(),
                p.getExternalUrl(),
                p.getOrderIndex(),
                p.getTestCases().stream().map(AdminTestCaseDto::from).toList()
        );
    }
}
