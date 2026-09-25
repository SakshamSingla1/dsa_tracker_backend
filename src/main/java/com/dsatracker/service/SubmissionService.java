package com.dsatracker.service;

import com.dsatracker.dto.SubmissionResponse;
import com.dsatracker.model.Submission;
import com.dsatracker.repository.SubmissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;

    public SubmissionService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getHistory(Long userId, Long problemId) {
        return submissionRepository.findAllByUserIdAndProblemIdOrderBySubmittedAtDesc(userId, problemId).stream()
                .map(SubmissionResponse::summary)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getOne(Long userId, Long submissionId) {
        Submission submission = submissionRepository.findByIdAndUserId(submissionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No submission with id " + submissionId));
        return SubmissionResponse.full(submission);
    }
}
