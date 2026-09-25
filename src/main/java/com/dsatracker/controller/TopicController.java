package com.dsatracker.controller;

import com.dsatracker.dto.ProblemOfDayResponse;
import com.dsatracker.dto.TopicResponse;
import com.dsatracker.model.User;
import com.dsatracker.service.TopicService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public List<TopicResponse> getTopics(@AuthenticationPrincipal User user, @RequestParam(required = false) String sheet) {
        return topicService.getAllTopics(user.getId(), sheet);
    }

    @GetMapping("/problem-of-the-day")
    public ProblemOfDayResponse getProblemOfTheDay(@AuthenticationPrincipal User user, @RequestParam(required = false) String sheet) {
        return topicService.getProblemOfTheDay(user.getId(), sheet);
    }
}
