package com.dsatracker.service;

import com.dsatracker.dto.AiCoachNoteResponse;
import com.dsatracker.dto.AiHintRequest;
import com.dsatracker.dto.AiHintResponse;
import com.dsatracker.dto.AiReviewRequest;
import com.dsatracker.dto.AiReviewResponse;
import com.dsatracker.dto.AnalyticsSummaryResponse;
import com.dsatracker.dto.TutorMessageRequest;
import com.dsatracker.dto.TutorMessageResponse;
import com.dsatracker.dto.TutorReplyResponse;
import com.dsatracker.model.ChatRole;
import com.dsatracker.model.Problem;
import com.dsatracker.model.TutorMessage;
import com.dsatracker.model.User;
import com.dsatracker.repository.ProblemRepository;
import com.dsatracker.repository.TutorMessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Prompt construction for the AI-powered hint/review/coach-note/tutor-chat features. All of them
 * degrade to {@code available=false} (not an error) when {@link GeminiService} isn't configured --
 * callers show a "not configured" state rather than breaking the surrounding feature.
 */
@Service
public class AiService {

    /** How many prior tutor-chat turns to replay as context -- enough for real back-and-forth
     *  without the prompt growing unbounded on a long-running conversation. */
    private static final int MAX_TUTOR_HISTORY_TURNS = 20;

    private final GeminiService geminiService;
    private final ProblemRepository problemRepository;
    private final AnalyticsService analyticsService;
    private final TutorMessageRepository tutorMessageRepository;

    public AiService(
            GeminiService geminiService,
            ProblemRepository problemRepository,
            AnalyticsService analyticsService,
            TutorMessageRepository tutorMessageRepository
    ) {
        this.geminiService = geminiService;
        this.problemRepository = problemRepository;
        this.analyticsService = analyticsService;
        this.tutorMessageRepository = tutorMessageRepository;
    }

    @Transactional(readOnly = true)
    public AiHintResponse getHint(Long problemId, AiHintRequest request) {
        Problem problem = findProblem(problemId);

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a terse, encouraging coding-interview coach. A student is solving this problem:\n\n");
        prompt.append("Title: ").append(problem.getTitle()).append('\n');
        prompt.append("Difficulty: ").append(problem.getDifficulty()).append('\n');
        if (problem.getStatement() != null && !problem.getStatement().isBlank()) {
            prompt.append("Statement: ").append(problem.getStatement()).append('\n');
        }
        if (!problem.getHints().isEmpty()) {
            prompt.append("Static hints already available to them:\n");
            for (String h : problem.getHints()) prompt.append("- ").append(h).append('\n');
        }
        if (request.code() != null && !request.code().isBlank()) {
            prompt.append("\nTheir current ").append(request.language() != null ? request.language() : "")
                    .append(" code attempt:\n```\n").append(request.code()).append("\n```\n");
        }
        prompt.append("""

                Give exactly ONE short hint (2-3 sentences max) that nudges them toward the next \
                step of the approach, WITHOUT giving away the full solution or writing code for them. \
                Don't restate the problem. Don't use markdown headers.""");

        return geminiService.generate(prompt.toString())
                .map(hint -> new AiHintResponse(true, hint))
                .orElseGet(() -> new AiHintResponse(false, null));
    }

    public AiReviewResponse getReview(AiReviewRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            return new AiReviewResponse(false, null);
        }

        String prompt = """
                You are a terse, friendly code reviewer. A student just submitted this %s solution \
                (judge verdict: %s):

                ```
                %s
                ```

                In 3-4 short sentences: note its actual time/space complexity, call out one concrete \
                thing they could improve (style, edge case, or efficiency) if there is one, and give \
                one word of encouragement if the verdict was ACCEPTED. No markdown headers, no restating \
                the code back to them.
                """.formatted(
                request.language() != null ? request.language() : "",
                request.verdict() != null ? request.verdict() : "UNKNOWN",
                request.code()
        );

        return geminiService.generate(prompt)
                .map(feedback -> new AiReviewResponse(true, feedback))
                .orElseGet(() -> new AiReviewResponse(false, null));
    }

    /**
     * A short, personalized "what to focus on" note built from the same stats that back the
     * Insights view -- layered on top of {@link AnalyticsService}'s rule-based recommendations,
     * not a replacement for them (that list keeps working even with AI disabled).
     */
    @Transactional(readOnly = true)
    public AiCoachNoteResponse getCoachNote(Long userId) {
        AnalyticsSummaryResponse summary = analyticsService.getSummary(userId, 90);
        if (summary.totalSubmissions() == 0) {
            return new AiCoachNoteResponse(true,
                    "You haven't submitted anything yet -- solve a problem or two and I'll have real feedback on where to focus.");
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a terse, encouraging coding-interview coach reviewing a student's practice stats.\n\n");
        prompt.append("Total submissions: ").append(summary.totalSubmissions()).append('\n');
        prompt.append("Accepted: ").append(summary.acceptedCount())
                .append(" (").append(Math.round(summary.acceptanceRate() * 100)).append("% acceptance)\n");
        prompt.append("Per-topic breakdown (accepted/attempted):\n");
        for (AnalyticsSummaryResponse.TopicBreakdown t : summary.byTopic()) {
            prompt.append("- ").append(t.topicName()).append(": ").append(t.accepted()).append('/').append(t.attempted()).append('\n');
        }
        prompt.append("""

                In 3-4 short sentences: call out their single weakest topic by name (lowest accepted/attempted \
                ratio, ignoring topics with very few attempts), give one concrete, encouraging suggestion for \
                what to focus on next, and end on a motivating note. No markdown headers, don't just restate \
                the numbers back to them.""");

        return geminiService.generate(prompt.toString())
                .map(note -> new AiCoachNoteResponse(true, note))
                .orElseGet(() -> new AiCoachNoteResponse(false, null));
    }

    @Transactional(readOnly = true)
    public List<TutorMessageResponse> getTutorHistory(Long userId, Long problemId) {
        return tutorMessageRepository.findAllByUserIdAndProblemIdOrderByCreatedAtAsc(userId, problemId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TutorReplyResponse sendTutorMessage(User user, Long problemId, TutorMessageRequest request) {
        Problem problem = findProblem(problemId);
        String content = request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        List<TutorMessage> history = tutorMessageRepository
                .findAllByUserIdAndProblemIdOrderByCreatedAtAsc(user.getId(), problemId);

        TutorMessage userMessage = new TutorMessage();
        userMessage.setUser(user);
        userMessage.setProblem(problem);
        userMessage.setRole(ChatRole.USER);
        userMessage.setContent(withCodeContext(content, request.code(), request.language()));
        tutorMessageRepository.save(userMessage);
        TutorMessageResponse userResponse = toResponse(userMessage);

        String systemInstruction = """
                You are a patient, encouraging coding-interview tutor helping a student with this problem:

                Title: %s
                Difficulty: %s
                Statement: %s

                Answer their questions about approach, complexity, or their code. Nudge them toward the \
                solution themselves rather than just handing it over -- ask a guiding question back if they \
                seem stuck, and only give directly-usable code if they explicitly ask for it. Keep replies \
                short (a few sentences), no markdown headers.
                """.formatted(
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getStatement() != null ? problem.getStatement() : ""
        );

        List<GeminiService.ChatTurn> turns = new ArrayList<>();
        int start = Math.max(0, history.size() - MAX_TUTOR_HISTORY_TURNS);
        for (TutorMessage m : history.subList(start, history.size())) {
            turns.add(m.getRole() == ChatRole.USER
                    ? GeminiService.ChatTurn.user(m.getContent())
                    : GeminiService.ChatTurn.model(m.getContent()));
        }
        turns.add(GeminiService.ChatTurn.user(userMessage.getContent()));

        Optional<String> reply = geminiService.generateChat(systemInstruction, turns);
        if (reply.isEmpty()) {
            return new TutorReplyResponse(false, userResponse, null);
        }

        TutorMessage assistantMessage = new TutorMessage();
        assistantMessage.setUser(user);
        assistantMessage.setProblem(problem);
        assistantMessage.setRole(ChatRole.ASSISTANT);
        assistantMessage.setContent(reply.get());
        tutorMessageRepository.save(assistantMessage);

        return new TutorReplyResponse(true, userResponse, toResponse(assistantMessage));
    }

    @Transactional
    public void clearTutorHistory(Long userId, Long problemId) {
        tutorMessageRepository.deleteAllByUserIdAndProblemId(userId, problemId);
    }

    private String withCodeContext(String content, String code, String language) {
        if (code == null || code.isBlank()) return content;
        return content + "\n\nTheir current " + (language != null ? language : "") + " code:\n```\n" + code + "\n```";
    }

    private TutorMessageResponse toResponse(TutorMessage m) {
        return new TutorMessageResponse(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt());
    }

    private Problem findProblem(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No problem with id " + id));
    }
}
