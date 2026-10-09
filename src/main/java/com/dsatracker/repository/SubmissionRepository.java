package com.dsatracker.repository;

import com.dsatracker.dto.SubmissionAnalyticsRow;
import com.dsatracker.model.Submission;
import com.dsatracker.model.User;
import com.dsatracker.model.Verdict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findAllByUserIdAndProblemIdOrderBySubmittedAtDesc(Long userId, Long problemId);
    Optional<Submission> findByIdAndUserId(Long id, Long userId);

    /** Lean projection of a user's full submission history for analytics aggregation (by
     *  verdict/language/topic/day) -- selects only the columns that aggregation actually reads,
     *  skipping Submission.code (a @Lob up to 50,000 chars/row) entirely. */
    @Query("select new com.dsatracker.dto.SubmissionAnalyticsRow(s.verdict, s.language, s.submittedAt, t.name) " +
            "from Submission s join s.problem p join p.topic t where s.user.id = :userId")
    List<SubmissionAnalyticsRow> findAnalyticsRowsByUserId(@Param("userId") Long userId);

    /** One row per user's distinct-problems-accepted count since {@code cutoff}, optionally scoped to
     *  one sheet -- backs the time-scoped (WEEK/MONTH) leaderboard views, since {@code UserProgress}
     *  only tracks the *first* completion date, not recent activity. */
    @Query("select s.user as user, count(distinct s.problem.id) as cnt from Submission s " +
            "where s.verdict = :verdict and s.submittedAt >= :cutoff " +
            "and (:sheetSlug is null or s.problem.topic.sheet.slug = :sheetSlug) " +
            "group by s.user")
    List<UserSolvedCount> countDistinctAcceptedProblemsSince(
            @Param("verdict") Verdict verdict, @Param("cutoff") Instant cutoff, @Param("sheetSlug") String sheetSlug);

    interface UserSolvedCount {
        User getUser();
        long getCnt();
    }

    void deleteAllByUserId(Long userId);

    /** Guards admin problem deletion -- a problem with recorded submissions can't be deleted
     *  without orphaning real history, so this backs a 409 rather than a FK-violation 500. */
    boolean existsByProblemId(Long problemId);

    long countBySubmittedAtAfter(Instant cutoff);
}
