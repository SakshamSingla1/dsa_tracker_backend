package com.dsatracker.dto;

import com.dsatracker.model.Topic;
import com.dsatracker.model.UserProgress;

import java.util.List;
import java.util.Map;

public record TopicResponse(
        Long id,
        String name,
        int orderIndex,
        List<ProblemResponse> problems
) {
    public static TopicResponse from(Topic t, Map<Long, UserProgress> progressByProblemId) {
        return from(t, progressByProblemId, null);
    }

    /** Same as {@link #from(Topic, Map)}, but prefixes the topic name -- e.g. for a cross-sheet view where topic names alone are ambiguous. */
    public static TopicResponse from(Topic t, Map<Long, UserProgress> progressByProblemId, String namePrefix) {
        List<ProblemResponse> problems = t.getProblems().stream()
                .map(p -> ProblemResponse.from(p, progressByProblemId.get(p.getId())))
                .toList();
        String name = namePrefix != null ? namePrefix + t.getName() : t.getName();
        return new TopicResponse(t.getId(), name, t.getOrderIndex(), problems);
    }
}
