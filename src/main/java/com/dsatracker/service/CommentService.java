package com.dsatracker.service;

import com.dsatracker.dto.CommentRequest;
import com.dsatracker.dto.CommentResponse;
import com.dsatracker.model.Comment;
import com.dsatracker.model.Problem;
import com.dsatracker.model.User;
import com.dsatracker.repository.CommentRepository;
import com.dsatracker.repository.ProblemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CommentService {

    private static final int MAX_BODY_LENGTH = 4000;

    private final CommentRepository commentRepository;
    private final ProblemRepository problemRepository;

    public CommentService(CommentRepository commentRepository, ProblemRepository problemRepository) {
        this.commentRepository = commentRepository;
        this.problemRepository = problemRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getForProblem(Long problemId, Long currentUserId) {
        return commentRepository.findAllByProblemIdOrderByCreatedAtAsc(problemId).stream()
                .map(c -> CommentResponse.from(c, currentUserId))
                .toList();
    }

    @Transactional
    public CommentResponse create(User user, Long problemId, CommentRequest request) {
        if (request.body() == null || request.body().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment can't be empty.");
        }
        String body = request.body().trim();
        if (body.length() > MAX_BODY_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must be " + MAX_BODY_LENGTH + " characters or fewer.");
        }
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No problem with id " + problemId));

        Comment comment = new Comment();
        comment.setProblem(problem);
        comment.setUser(user);
        comment.setBody(body);
        comment = commentRepository.save(comment);
        return CommentResponse.from(comment, user.getId());
    }

    @Transactional
    public void delete(User user, Long commentId) {
        Comment comment = commentRepository.findByIdAndUserId(commentId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No comment with id " + commentId));
        commentRepository.delete(comment);
    }
}
