package com.dsatracker.controller.admin;

import com.dsatracker.dto.admin.AdminProblemRequest;
import com.dsatracker.dto.admin.AdminProblemResponse;
import com.dsatracker.dto.admin.AdminProblemSummaryResponse;
import com.dsatracker.service.admin.AdminProblemService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/problems")
@PreAuthorize("hasAuthority('PROBLEM_MANAGE')")
public class AdminProblemController {

    private final AdminProblemService adminProblemService;

    public AdminProblemController(AdminProblemService adminProblemService) {
        this.adminProblemService = adminProblemService;
    }

    @GetMapping
    public List<AdminProblemSummaryResponse> list() {
        return adminProblemService.listProblems();
    }

    @GetMapping("/{id}")
    public AdminProblemResponse get(@PathVariable Long id) {
        return adminProblemService.getProblem(id);
    }

    @PostMapping
    public AdminProblemResponse create(@RequestBody AdminProblemRequest request) {
        return adminProblemService.createProblem(request);
    }

    @PutMapping("/{id}")
    public AdminProblemResponse update(@PathVariable Long id, @RequestBody AdminProblemRequest request) {
        return adminProblemService.updateProblem(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        adminProblemService.deleteProblem(id);
    }
}
