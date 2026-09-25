package com.dsatracker.controller;

import com.dsatracker.dto.LeaderboardEntry;
import com.dsatracker.model.User;
import com.dsatracker.service.LeaderboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public List<LeaderboardEntry> getLeaderboard(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "ALL") String scope,
            @RequestParam(required = false) String sheet) {
        return leaderboardService.getLeaderboard(user.getId(), scope, sheet);
    }
}
