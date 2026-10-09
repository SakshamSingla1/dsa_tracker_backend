package com.dsatracker.service.admin;

import com.dsatracker.dto.admin.AdminTopicReorderRequest;
import com.dsatracker.model.Problem;
import com.dsatracker.model.Sheet;
import com.dsatracker.model.Topic;
import com.dsatracker.repository.SheetRepository;
import com.dsatracker.repository.TopicRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTopicServiceTest {

    @Mock TopicRepository topicRepository;
    @Mock SheetRepository sheetRepository;

    private AdminTopicService adminTopicService;

    @BeforeEach
    void setUp() {
        adminTopicService = new AdminTopicService(topicRepository, sheetRepository);
    }

    private Topic topicWithId(long id) {
        Topic topic = new Topic();
        topic.setId(id);
        return topic;
    }

    @Test
    void deleteTopic_rejectsWhenProblemsExist() {
        Topic topic = topicWithId(1L);
        topic.getProblems().add(new Problem());
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        assertThatThrownBy(() -> adminTopicService.deleteTopic(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Remove every problem");
    }

    @Test
    void reorderTopics_rejectsWhenAnIdIsMissingFromTheRequest() {
        Topic t1 = topicWithId(1L);
        Topic t2 = topicWithId(2L);
        when(topicRepository.findAllBySheetIdOrderByOrderIndexAsc(9L)).thenReturn(List.of(t1, t2));

        assertThatThrownBy(() -> adminTopicService.reorderTopics(new AdminTopicReorderRequest(9L, List.of(1L))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exactly once");
    }

    @Test
    void reorderTopics_assignsSequentialOrderIndexMatchingRequestedOrder() {
        Topic t1 = topicWithId(1L);
        Topic t2 = topicWithId(2L);
        when(topicRepository.findAllBySheetIdOrderByOrderIndexAsc(9L)).thenReturn(List.of(t1, t2));
        when(topicRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        adminTopicService.reorderTopics(new AdminTopicReorderRequest(9L, List.of(2L, 1L)));

        assertThat(t2.getOrderIndex()).isEqualTo(0);
        assertThat(t1.getOrderIndex()).isEqualTo(1);
    }
}
