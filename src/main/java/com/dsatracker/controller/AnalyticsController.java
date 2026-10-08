package com.dsatracker.controller;

import com.dsatracker.dto.AnalyticsSummaryResponse;
import com.dsatracker.dto.RecommendationResponse;
import com.dsatracker.model.User;
import com.dsatracker.service.AnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsSummaryResponse getSummary(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "90") int days
    ) {
        return analyticsService.getSummary(user.getId(), days);
    }

    /** Rule-based "what to solve next" -- weak topics first, unexplored topics as a fallback. */
    @GetMapping("/recommendations")
    public List<RecommendationResponse> getRecommendations(@AuthenticationPrincipal User user) {
        return analyticsService.getRecommendations(user.getId());
    }
}
