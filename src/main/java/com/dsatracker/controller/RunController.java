package com.dsatracker.controller;

import com.dsatracker.dto.RunRequest;
import com.dsatracker.dto.RunResponse;
import com.dsatracker.service.CodeRunnerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/run")
public class RunController {

    private final CodeRunnerService codeRunnerService;

    public RunController(CodeRunnerService codeRunnerService) {
        this.codeRunnerService = codeRunnerService;
    }

    @PostMapping
    public RunResponse run(@RequestBody RunRequest request) {
        return codeRunnerService.run(request);
    }
}
