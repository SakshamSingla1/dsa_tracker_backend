package com.dsatracker.controller;

import com.dsatracker.dto.TopicResponse;
import com.dsatracker.model.User;
import com.dsatracker.service.ProgressService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/summary")
    public List<TopicResponse> getSummary(@AuthenticationPrincipal User user, @RequestParam(required = false) String sheet) {
        return progressService.getSummary(user.getId(), sheet);
    }
}
