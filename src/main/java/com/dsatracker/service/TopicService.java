package com.dsatracker.service;

import com.dsatracker.dto.ProblemOfDayResponse;
import com.dsatracker.dto.ProblemResponse;
import com.dsatracker.dto.TopicResponse;
import com.dsatracker.model.Problem;
import com.dsatracker.model.Topic;
import com.dsatracker.model.UserProgress;
import com.dsatracker.repository.TopicRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TopicService {

    private final TopicRepository topicRepository;
    private final UserProgressRepository userProgressRepository;

    public TopicService(TopicRepository topicRepository, UserProgressRepository userProgressRepository) {
        this.topicRepository = topicRepository;
        this.userProgressRepository = userProgressRepository;
    }

    /** Default sheet used when the caller doesn't ask for a specific one, e.g. the original A2Z sheet. */
    private static final String DEFAULT_SHEET_SLUG = "a2z";

    @Transactional(readOnly = true)
    public List<TopicResponse> getAllTopics(Long userId, String sheetSlug) {
        Map<Long, UserProgress> progressByProblemId = userProgressRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(up -> up.getProblem().getId(), Function.identity()));

        String slug = sheetSlug != null && !sheetSlug.isBlank() ? sheetSlug : DEFAULT_SHEET_SLUG;
        return topicRepository.findAllBySheetSlugOrderByOrderIndexAsc(slug).stream()
                .map(topic -> TopicResponse.from(topic, progressByProblemId))
                .toList();
    }

    /**
     * Deterministically picks the same problem for everyone on a given sheet, for a given
     * calendar day -- no state to store, and it naturally rotates through the whole sheet
     * before repeating (the day-of-epoch mod problem count).
     */
    @Transactional(readOnly = true)
    public ProblemOfDayResponse getProblemOfTheDay(Long userId, String sheetSlug) {
        String slug = sheetSlug != null && !sheetSlug.isBlank() ? sheetSlug : DEFAULT_SHEET_SLUG;
        List<Topic> topics = topicRepository.findAllBySheetSlugOrderByOrderIndexAsc(slug);

        record Entry(Problem problem, Topic topic) {
        }
        List<Entry> flat = topics.stream()
                .flatMap(topic -> topic.getProblems().stream().map(p -> new Entry(p, topic)))
                .toList();

        if (flat.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No problems on sheet " + slug);
        }

        long epochDay = LocalDate.now().toEpochDay();
        int index = Math.floorMod(epochDay, flat.size());
        Entry chosen = flat.get(index);

        UserProgress progress = userProgressRepository.findByUserIdAndProblemId(userId, chosen.problem().getId()).orElse(null);
        return new ProblemOfDayResponse(ProblemResponse.from(chosen.problem(), progress), chosen.topic().getName(), chosen.topic().getId());
    }
}
