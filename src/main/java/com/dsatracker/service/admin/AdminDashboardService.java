package com.dsatracker.service.admin;

import com.dsatracker.dto.admin.AdminDashboardStatsResponse;
import com.dsatracker.repository.ProblemRepository;
import com.dsatracker.repository.SheetRepository;
import com.dsatracker.repository.SubmissionRepository;
import com.dsatracker.repository.TopicRepository;
import com.dsatracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final SheetRepository sheetRepository;
    private final TopicRepository topicRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            SheetRepository sheetRepository,
            TopicRepository topicRepository,
            ProblemRepository problemRepository,
            SubmissionRepository submissionRepository
    ) {
        this.userRepository = userRepository;
        this.sheetRepository = sheetRepository;
        this.topicRepository = topicRepository;
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getStats() {
        return new AdminDashboardStatsResponse(
                userRepository.count(),
                sheetRepository.count(),
                topicRepository.count(),
                problemRepository.count(),
                submissionRepository.countBySubmittedAtAfter(Instant.now().minus(7, ChronoUnit.DAYS)),
                userRepository.countByRoleIsNotNullAndEnabledTrue()
        );
    }
}
