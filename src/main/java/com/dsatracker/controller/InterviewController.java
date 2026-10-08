package com.dsatracker.controller;

import com.dsatracker.dto.InterviewMessageRequest;
import com.dsatracker.dto.InterviewSessionResponse;
import com.dsatracker.dto.StartInterviewRequest;
import com.dsatracker.model.User;
import com.dsatracker.service.InterviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping
    public InterviewSessionResponse start(@AuthenticationPrincipal User user, @RequestBody(required = false) StartInterviewRequest request) {
        return interviewService.start(user, request != null ? request : new StartInterviewRequest(null, null));
    }

    @GetMapping("/history")
    public List<InterviewSessionResponse> history(@AuthenticationPrincipal User user) {
        return interviewService.history(user);
    }

    @GetMapping("/{id}")
    public InterviewSessionResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return interviewService.get(user, id);
    }

    @PostMapping("/{id}/messages")
    public InterviewSessionResponse postMessage(
            @AuthenticationPrincipal User user, @PathVariable Long id, @RequestBody InterviewMessageRequest request
    ) {
        return interviewService.postMessage(user, id, request);
    }

    @PostMapping("/{id}/end")
    public InterviewSessionResponse end(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return interviewService.end(user, id);
    }
}
