package com.dsatracker.service.admin;

import com.dsatracker.dto.admin.AdminTopicReorderRequest;
import com.dsatracker.dto.admin.AdminTopicRequest;
import com.dsatracker.dto.admin.AdminTopicResponse;
import com.dsatracker.model.Sheet;
import com.dsatracker.model.Topic;
import com.dsatracker.repository.SheetRepository;
import com.dsatracker.repository.TopicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminTopicService {

    private final TopicRepository topicRepository;
    private final SheetRepository sheetRepository;

    public AdminTopicService(TopicRepository topicRepository, SheetRepository sheetRepository) {
        this.topicRepository = topicRepository;
        this.sheetRepository = sheetRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminTopicResponse> listTopics() {
        return topicRepository.findAllByOrderBySheet_OrderIndexAscOrderIndexAsc().stream()
                .map(AdminTopicResponse::from)
                .toList();
    }

    @Transactional
    public AdminTopicResponse createTopic(AdminTopicRequest request) {
        validate(request);
        Sheet sheet = findSheet(request.sheetId());
        Topic topic = new Topic();
        topic.setSheet(sheet);
        topic.setName(request.name().trim());
        topic.setOrderIndex(request.orderIndex());
        return AdminTopicResponse.from(topicRepository.save(topic));
    }

    @Transactional
    public AdminTopicResponse updateTopic(Long id, AdminTopicRequest request) {
        validate(request);
        Topic topic = findTopic(id);
        topic.setSheet(findSheet(request.sheetId()));
        topic.setName(request.name().trim());
        topic.setOrderIndex(request.orderIndex());
        return AdminTopicResponse.from(topicRepository.save(topic));
    }

    @Transactional
    public void deleteTopic(Long id) {
        Topic topic = findTopic(id);
        if (!topic.getProblems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Remove every problem from this topic before deleting it.");
        }
        topicRepository.delete(topic);
    }

    @Transactional
    public List<AdminTopicResponse> reorderTopics(AdminTopicReorderRequest request) {
        if (request.sheetId() == null || request.orderedTopicIds() == null || request.orderedTopicIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sheetId and orderedTopicIds are required.");
        }
        List<Topic> existing = topicRepository.findAllBySheetIdOrderByOrderIndexAsc(request.sheetId());
        Map<Long, Topic> byId = existing.stream().collect(Collectors.toMap(Topic::getId, Function.identity()));

        Set<Long> requestedIds = new HashSet<>(request.orderedTopicIds());
        if (requestedIds.size() != request.orderedTopicIds().size() || !requestedIds.equals(byId.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderedTopicIds must list every topic of this sheet exactly once.");
        }

        for (int i = 0; i < request.orderedTopicIds().size(); i++) {
            byId.get(request.orderedTopicIds().get(i)).setOrderIndex(i);
        }
        return topicRepository.saveAll(existing).stream().map(AdminTopicResponse::from).toList();
    }

    private void validate(AdminTopicRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        }
        if (request.sheetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sheetId is required.");
        }
    }

    private Topic findTopic(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No topic with id " + id));
    }

    private Sheet findSheet(Long sheetId) {
        return sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No sheet with id " + sheetId));
    }
}
