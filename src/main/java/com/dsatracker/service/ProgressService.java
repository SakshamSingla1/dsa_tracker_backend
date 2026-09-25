package com.dsatracker.service;

import com.dsatracker.dto.TopicResponse;
import com.dsatracker.model.UserProgress;
import com.dsatracker.repository.TopicRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProgressService {

    private final TopicService topicService;
    private final TopicRepository topicRepository;
    private final UserProgressRepository userProgressRepository;

    public ProgressService(TopicService topicService, TopicRepository topicRepository, UserProgressRepository userProgressRepository) {
        this.topicService = topicService;
        this.topicRepository = topicRepository;
        this.userProgressRepository = userProgressRepository;
    }

    /**
     * {@code sheetSlug == "all"} rolls every sheet's topics into one list (topic names prefixed
     * with their sheet's name), so the dashboard can compute streaks/achievements that aren't
     * reset by switching sheets. Any other value (including null) behaves exactly like
     * {@link TopicService#getAllTopics}.
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> getSummary(Long userId, String sheetSlug) {
        if (!"all".equalsIgnoreCase(sheetSlug)) {
            return topicService.getAllTopics(userId, sheetSlug);
        }

        Map<Long, UserProgress> progressByProblemId = userProgressRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(up -> up.getProblem().getId(), Function.identity()));

        return topicRepository.findAllByOrderBySheet_OrderIndexAscOrderIndexAsc().stream()
                .map(topic -> TopicResponse.from(topic, progressByProblemId, topic.getSheet().getName() + " • "))
                .toList();
    }
}
