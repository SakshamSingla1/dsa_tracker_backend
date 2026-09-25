package com.dsatracker.controller;

import com.dsatracker.dto.ContestSessionResponse;
import com.dsatracker.dto.StartContestRequest;
import com.dsatracker.model.User;
import com.dsatracker.service.ContestService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    @PostMapping
    public ContestSessionResponse start(@AuthenticationPrincipal User user, @RequestBody StartContestRequest request) {
        return contestService.start(user, request);
    }

    @GetMapping("/history")
    public List<ContestSessionResponse> history(@AuthenticationPrincipal User user) {
        return contestService.history(user);
    }

    @GetMapping("/{id}")
    public ContestSessionResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return contestService.get(user, id);
    }

    @PostMapping("/{id}/finish")
    public ContestSessionResponse finish(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return contestService.finish(user, id);
    }
}
