package com.dsatracker.dto;

import com.dsatracker.model.Difficulty;
import com.dsatracker.model.Problem;
import com.dsatracker.model.Status;
import com.dsatracker.model.UserProgress;

import java.time.LocalDate;
import java.util.List;

/**
 * Lightweight problem view for list endpoints (topics/progress-summary), which render every
 * problem in a sheet at once. Deliberately omits {@code examples}/{@code constraints}/{@code hints}
 * (separate @ElementCollection tables -- each one is an extra batched round trip for the whole
 * sheet, several seconds of added latency on a cross-region DB) and {@code sampleTests}/
 * {@code totalTestCases} (forces loading the testCases association). None of those are rendered
 * by a collapsed/list problem row anyway -- only the Solve view needs them, and it fetches a
 * single problem's full {@link ProblemResponse} on demand instead.
 */
public record ProblemSummaryResponse(
        Long id,
        String title,
        Difficulty difficulty,
        String statement,
        String timeComplexity,
        String spaceComplexity,
        String exampleInput,
        String exampleOutput,
        String editorialUrl,
        String videoUrl,
        List<String> tags,
        String externalUrl,
        Status status,
        String notes,
        LocalDate completedAt,
        LocalDate reviewDueAt,
        boolean bookmarked,
        int orderIndex
) {
    /** progress may be null -- a problem the current user hasn't touched yet is implicitly TODO. */
    public static ProblemSummaryResponse from(Problem p, UserProgress progress) {
        return new ProblemSummaryResponse(
                p.getId(),
                p.getTitle(),
                p.getDifficulty(),
                p.getStatement(),
                p.getTimeComplexity(),
                p.getSpaceComplexity(),
                p.getExampleInput(),
                p.getExampleOutput(),
                p.getEditorialUrl(),
                p.getVideoUrl(),
                p.getTags(),
                p.getExternalUrl(),
                progress != null ? progress.getStatus() : Status.TODO,
                progress != null ? progress.getNotes() : null,
                progress != null ? progress.getCompletedAt() : null,
                progress != null ? progress.getReviewDueAt() : null,
                progress != null && progress.isBookmarked(),
                p.getOrderIndex()
        );
    }
}
