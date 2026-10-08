package com.dsatracker.controller;

import com.dsatracker.dto.CommentRequest;
import com.dsatracker.dto.CommentResponse;
import com.dsatracker.model.User;
import com.dsatracker.service.CommentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/api/problems/{id}/comments")
    public List<CommentResponse> getComments(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return commentService.getForProblem(id, user.getId());
    }

    @PostMapping("/api/problems/{id}/comments")
    public CommentResponse createComment(
            @AuthenticationPrincipal User user, @PathVariable Long id, @RequestBody CommentRequest request
    ) {
        return commentService.create(user, id, request);
    }

    @DeleteMapping("/api/comments/{commentId}")
    public void deleteComment(@AuthenticationPrincipal User user, @PathVariable Long commentId) {
        commentService.delete(user, commentId);
    }
}
