package com.dsatracker.repository;

import com.dsatracker.model.Status;
import com.dsatracker.model.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    List<UserProgress> findAllByUserId(Long userId);
    Optional<UserProgress> findByUserIdAndProblemId(Long userId, Long problemId);
    List<UserProgress> findAllByStatus(Status status);
    List<UserProgress> findAllByUserIdAndStatus(Long userId, Status status);

    /** Same as {@link #findAllByStatus} but scoped to one sheet, for the leaderboard's per-sheet view. */
    List<UserProgress> findAllByStatusAndProblem_Topic_Sheet_Slug(Status status, String sheetSlug);

    void deleteAllByUserId(Long userId);
}
