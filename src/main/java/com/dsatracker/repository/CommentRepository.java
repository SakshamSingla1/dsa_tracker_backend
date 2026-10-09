package com.dsatracker.repository;

import com.dsatracker.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByProblemIdOrderByCreatedAtAsc(Long problemId);
    Optional<Comment> findByIdAndUserId(Long id, Long userId);

    void deleteAllByUserId(Long userId);

    /** Guards admin problem deletion -- see {@link SubmissionRepository#existsByProblemId}. */
    boolean existsByProblemId(Long problemId);
}
