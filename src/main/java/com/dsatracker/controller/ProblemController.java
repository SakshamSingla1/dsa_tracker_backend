package com.dsatracker.controller;

import com.dsatracker.dto.ProblemOfDayResponse;
import com.dsatracker.dto.ProblemResponse;
import com.dsatracker.dto.ProblemUpdateRequest;
import com.dsatracker.dto.ReviewRequest;
import com.dsatracker.model.User;
import com.dsatracker.service.ProblemService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    /** Fetches a single problem regardless of the currently active sheet -- used when a contest
     *  draws a problem from a sheet the client hasn't loaded topics for. */
    @GetMapping("/{id}")
    public ProblemOfDayResponse getProblem(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return problemService.getById(user, id);
    }

    @PatchMapping("/{id}")
    public ProblemResponse updateProblem(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody ProblemUpdateRequest request
    ) {
        return problemService.update(user, id, request);
    }

    @PostMapping("/{id}/review")
    public ProblemResponse reviewProblem(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody ReviewRequest request
    ) {
        return problemService.review(user, id, request.outcome());
    }
}
