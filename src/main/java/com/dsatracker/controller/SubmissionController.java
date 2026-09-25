package com.dsatracker.controller;

import com.dsatracker.dto.SubmissionResponse;
import com.dsatracker.model.User;
import com.dsatracker.service.SubmissionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping
    public List<SubmissionResponse> getHistory(@AuthenticationPrincipal User user, @RequestParam Long problemId) {
        return submissionService.getHistory(user.getId(), problemId);
    }

    @GetMapping("/{id}")
    public SubmissionResponse getOne(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return submissionService.getOne(user.getId(), id);
    }
}
