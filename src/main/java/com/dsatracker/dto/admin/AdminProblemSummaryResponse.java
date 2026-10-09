package com.dsatracker.dto.admin;

import com.dsatracker.model.Difficulty;
import com.dsatracker.model.Problem;

/** Lightweight row for the admin problem list screen -- omits examples/constraints/hints/
 *  testCases, same reasoning as {@link com.dsatracker.dto.ProblemSummaryResponse}. The edit
 *  screen fetches one problem's full {@link AdminProblemResponse} on demand instead. */
public record AdminProblemSummaryResponse(
        Long id, String title, Difficulty difficulty, Long topicId, String topicName, int orderIndex
) {
    public static AdminProblemSummaryResponse from(Problem p) {
        return new AdminProblemSummaryResponse(
                p.getId(), p.getTitle(), p.getDifficulty(),
                p.getTopic() != null ? p.getTopic().getId() : null,
                p.getTopic() != null ? p.getTopic().getName() : null,
                p.getOrderIndex()
        );
    }
}
