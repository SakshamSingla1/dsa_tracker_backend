package com.dsatracker.service.admin;

import com.dsatracker.dto.admin.AdminProblemRequest;
import com.dsatracker.dto.admin.AdminTestCaseDto;
import com.dsatracker.model.Difficulty;
import com.dsatracker.model.Problem;
import com.dsatracker.model.TestCase;
import com.dsatracker.model.Topic;
import com.dsatracker.repository.CommentRepository;
import com.dsatracker.repository.ProblemRepository;
import com.dsatracker.repository.SubmissionRepository;
import com.dsatracker.repository.TopicRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProblemServiceTest {

    @Mock ProblemRepository problemRepository;
    @Mock TopicRepository topicRepository;
    @Mock UserProgressRepository userProgressRepository;
    @Mock SubmissionRepository submissionRepository;
    @Mock CommentRepository commentRepository;

    private AdminProblemService adminProblemService;

    @BeforeEach
    void setUp() {
        adminProblemService = new AdminProblemService(
                problemRepository, topicRepository, userProgressRepository, submissionRepository, commentRepository);
    }

    private Topic topicWithId(long id) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setName("Arrays");
        return topic;
    }

    private AdminProblemRequest requestWithTestCases(List<AdminTestCaseDto> testCases) {
        return new AdminProblemRequest(
                1L, "Two Sum", Difficulty.EASY, "statement", "O(n)", "O(n)",
                List.of(), List.of(), List.of(), null, null, List.of(), null, 0, testCases
        );
    }

    @Test
    void deleteProblem_rejectsWhenUserProgressExists() {
        Problem problem = new Problem();
        problem.setId(10L);
        when(problemRepository.findById(10L)).thenReturn(Optional.of(problem));
        when(userProgressRepository.existsByProblemId(10L)).thenReturn(true);

        assertThatThrownBy(() -> adminProblemService.deleteProblem(10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("recorded progress, submissions, or comments");
    }

    @Test
    void deleteProblem_succeedsWhenNoHistoryExists() {
        Problem problem = new Problem();
        problem.setId(11L);
        when(problemRepository.findById(11L)).thenReturn(Optional.of(problem));
        when(userProgressRepository.existsByProblemId(11L)).thenReturn(false);
        when(submissionRepository.existsByProblemId(11L)).thenReturn(false);
        when(commentRepository.existsByProblemId(11L)).thenReturn(false);

        adminProblemService.deleteProblem(11L);
        // no exception -- reaching here is the assertion
    }

    @Test
    void createProblem_rejectsUnknownTopicId() {
        when(topicRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminProblemService.createProblem(requestWithTestCases(List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No topic with id 1");
    }

    @Test
    void updateProblem_reconcilesTestCases_updatingExistingAndAddingNew() {
        Problem problem = new Problem();
        problem.setId(5L);
        TestCase existing = new TestCase();
        existing.setId(100L);
        existing.setInput("old-in");
        existing.setExpectedOutput("old-out");
        problem.getTestCases().add(existing);

        when(problemRepository.findById(5L)).thenReturn(Optional.of(problem));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topicWithId(1L)));
        when(problemRepository.save(any(Problem.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = requestWithTestCases(List.of(
                new AdminTestCaseDto(100L, "new-in", "new-out", true),
                new AdminTestCaseDto(null, "second-in", "second-out", false)
        ));

        var response = adminProblemService.updateProblem(5L, request);

        assertThat(response.testCases()).hasSize(2);
        assertThat(problem.getTestCases()).hasSize(2);
        // The existing entity (id=100) was updated in place, not replaced.
        assertThat(problem.getTestCases().get(0)).isSameAs(existing);
        assertThat(existing.getInput()).isEqualTo("new-in");
    }

    @Test
    void updateProblem_removesTestCaseOmittedFromIncomingList() {
        Problem problem = new Problem();
        problem.setId(5L);
        TestCase toRemove = new TestCase();
        toRemove.setId(200L);
        problem.getTestCases().add(toRemove);

        when(problemRepository.findById(5L)).thenReturn(Optional.of(problem));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topicWithId(1L)));
        when(problemRepository.save(any(Problem.class))).thenAnswer(inv -> inv.getArgument(0));

        adminProblemService.updateProblem(5L, requestWithTestCases(List.of()));

        assertThat(problem.getTestCases()).isEmpty();
    }
}
