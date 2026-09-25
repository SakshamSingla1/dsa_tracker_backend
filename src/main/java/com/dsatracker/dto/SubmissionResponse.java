package com.dsatracker.dto;

import com.dsatracker.model.Language;
import com.dsatracker.model.Submission;
import com.dsatracker.model.Verdict;

import java.time.Instant;

/** One row of submission history. {@code code} is included only when fetching a single submission. */
public record SubmissionResponse(
        Long id,
        Long problemId,
        Language language,
        Verdict verdict,
        int passedCount,
        int totalCount,
        long runtimeMs,
        Instant submittedAt,
        String code
) {
    public static SubmissionResponse summary(Submission s) {
        return new SubmissionResponse(
                s.getId(), s.getProblem().getId(), s.getLanguage(), s.getVerdict(),
                s.getPassedCount(), s.getTotalCount(), s.getRuntimeMs(), s.getSubmittedAt(), null
        );
    }

    public static SubmissionResponse full(Submission s) {
        return new SubmissionResponse(
                s.getId(), s.getProblem().getId(), s.getLanguage(), s.getVerdict(),
                s.getPassedCount(), s.getTotalCount(), s.getRuntimeMs(), s.getSubmittedAt(), s.getCode()
        );
    }
}
