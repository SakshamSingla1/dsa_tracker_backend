package com.dsatracker.repository;

import com.dsatracker.dto.ProblemRecommendationRow;
import com.dsatracker.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    /** Lean projection across every problem in the catalog, for the recommendation engine's
     *  full scan (see ProblemRecommendationRow) -- skips tags and every other collection a full
     *  entity fetch would otherwise eagerly load for all of them. */
    @Query("select new com.dsatracker.dto.ProblemRecommendationRow(p.id, p.title, p.difficulty, t.name) " +
            "from Problem p join p.topic t")
    List<ProblemRecommendationRow> findAllForRecommendations();
}
