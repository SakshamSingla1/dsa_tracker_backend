package com.dsatracker.controller;

import com.dsatracker.dto.JudgeRequest;
import com.dsatracker.dto.JudgeResponse;
import com.dsatracker.model.User;
import com.dsatracker.service.JudgeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems/{id}")
public class JudgeController {

    private final JudgeService judgeService;

    public JudgeController(JudgeService judgeService) {
        this.judgeService = judgeService;
    }

    /** Runs the sample test cases only -- nothing is persisted. */
    @PostMapping("/run-tests")
    public JudgeResponse runTests(@PathVariable Long id, @RequestBody JudgeRequest request) {
        return judgeService.run(id, request);
    }

    /** Runs every test case (sample + hidden), records the attempt, and marks the problem DONE on ACCEPTED. */
    @PostMapping("/submit")
    public JudgeResponse submit(@AuthenticationPrincipal User user, @PathVariable Long id, @RequestBody JudgeRequest request) {
        return judgeService.submit(user, id, request);
    }
}
