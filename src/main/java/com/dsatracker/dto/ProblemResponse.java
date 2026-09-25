package com.dsatracker.dto;

import com.dsatracker.model.Difficulty;
import com.dsatracker.model.Problem;
import com.dsatracker.model.Status;
import com.dsatracker.model.UserProgress;

import java.time.LocalDate;
import java.util.List;

public record ProblemResponse(
        Long id,
        String title,
        Difficulty difficulty,
        String statement,
        String timeComplexity,
        String spaceComplexity,
        String exampleInput,
        String exampleOutput,
        List<ExampleResponse> examples,
        List<String> constraints,
        List<String> hints,
        String editorialUrl,
        String videoUrl,
        List<String> tags,
        String externalUrl,
        Status status,
        String notes,
        LocalDate completedAt,
        LocalDate reviewDueAt,
        boolean bookmarked,
        int orderIndex,
        List<SampleTestCase> sampleTests,
        int totalTestCases
) {
    /** progress may be null -- a problem the current user hasn't touched yet is implicitly TODO. */
    public static ProblemResponse from(Problem p, UserProgress progress) {
        List<SampleTestCase> sampleTests = p.getTestCases().stream()
                .filter(tc -> tc.isSample())
                .map(tc -> new SampleTestCase(tc.getInput(), tc.getExpectedOutput()))
                .toList();

        return new ProblemResponse(
                p.getId(),
                p.getTitle(),
                p.getDifficulty(),
                p.getStatement(),
                p.getTimeComplexity(),
                p.getSpaceComplexity(),
                p.getExampleInput(),
                p.getExampleOutput(),
                p.getExamples().stream().map(ExampleResponse::from).toList(),
                p.getConstraints(),
                p.getHints(),
                p.getEditorialUrl(),
                p.getVideoUrl(),
                p.getTags(),
                p.getExternalUrl(),
                progress != null ? progress.getStatus() : Status.TODO,
                progress != null ? progress.getNotes() : null,
                progress != null ? progress.getCompletedAt() : null,
                progress != null ? progress.getReviewDueAt() : null,
                progress != null && progress.isBookmarked(),
                p.getOrderIndex(),
                sampleTests,
                p.getTestCases().size()
        );
    }
}
