package com.dsatracker.controller.admin;

import com.dsatracker.dto.admin.AdminTopicReorderRequest;
import com.dsatracker.dto.admin.AdminTopicRequest;
import com.dsatracker.dto.admin.AdminTopicResponse;
import com.dsatracker.service.admin.AdminTopicService;
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
@RequestMapping("/api/admin/topics")
@PreAuthorize("hasAuthority('TOPIC_MANAGE')")
public class AdminTopicController {

    private final AdminTopicService adminTopicService;

    public AdminTopicController(AdminTopicService adminTopicService) {
        this.adminTopicService = adminTopicService;
    }

    @GetMapping
    public List<AdminTopicResponse> list() {
        return adminTopicService.listTopics();
    }

    @PostMapping
    public AdminTopicResponse create(@RequestBody AdminTopicRequest request) {
        return adminTopicService.createTopic(request);
    }

    @PutMapping("/{id}")
    public AdminTopicResponse update(@PathVariable Long id, @RequestBody AdminTopicRequest request) {
        return adminTopicService.updateTopic(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        adminTopicService.deleteTopic(id);
    }

    @PostMapping("/reorder")
    public List<AdminTopicResponse> reorder(@RequestBody AdminTopicReorderRequest request) {
        return adminTopicService.reorderTopics(request);
    }
}
