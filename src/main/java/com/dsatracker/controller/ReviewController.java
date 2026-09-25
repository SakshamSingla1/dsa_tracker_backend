package com.dsatracker.controller;

import com.dsatracker.dto.ProblemResponse;
import com.dsatracker.model.User;
import com.dsatracker.service.ReviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/queue")
    public List<ProblemResponse> getQueue(@AuthenticationPrincipal User user) {
        return reviewService.getDueQueue(user.getId());
    }
}
