package com.dsatracker.service;

import com.dsatracker.dto.InterviewMessageRequest;
import com.dsatracker.dto.InterviewMessageResponse;
import com.dsatracker.dto.InterviewSessionResponse;
import com.dsatracker.dto.StartInterviewRequest;
import com.dsatracker.model.ChatRole;
import com.dsatracker.model.Difficulty;
import com.dsatracker.model.InterviewMessage;
import com.dsatracker.model.InterviewSession;
import com.dsatracker.model.InterviewStatus;
import com.dsatracker.model.Problem;
import com.dsatracker.model.Status;
import com.dsatracker.model.User;
import com.dsatracker.repository.InterviewMessageRepository;
import com.dsatracker.repository.InterviewSessionRepository;
import com.dsatracker.repository.ProblemRepository;
import com.dsatracker.repository.UserProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * AI-run mock interviews: the AI stays in character as an interviewer across a back-and-forth
 * tied to one drawn problem (see {@link InterviewSession}/{@link InterviewMessage}), then gives
 * a final verdict when the session ends. Degrades to a static opening/closing message (not an
 * error) when {@link GeminiService} isn't configured, same as the rest of the AI feature set.
 */
@Service
public class InterviewService {

    /** How many prior turns to replay as context, same reasoning as AiService's tutor chat. */
    private static final int MAX_HISTORY_TURNS = 30;

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewMessageRepository interviewMessageRepository;
    private final ProblemRepository problemRepository;
    private final UserProgressRepository userProgressRepository;
    private final GeminiService geminiService;

    public InterviewService(
            InterviewSessionRepository interviewSessionRepository,
            InterviewMessageRepository interviewMessageRepository,
            ProblemRepository problemRepository,
            UserProgressRepository userProgressRepository,
            GeminiService geminiService
    ) {
        this.interviewSessionRepository = interviewSessionRepository;
        this.interviewMessageRepository = interviewMessageRepository;
        this.problemRepository = problemRepository;
        this.userProgressRepository = userProgressRepository;
        this.geminiService = geminiService;
    }

    @Transactional
    public InterviewSessionResponse start(User user, StartInterviewRequest request) {
        Problem problem = pickProblem(user, request);

        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setProblem(problem);
        session = interviewSessionRepository.save(session);

        Optional<String> opening = geminiService.generateChat(
                interviewerSystemInstruction(problem),
                List.of(GeminiService.ChatTurn.user("Begin the interview."))
        );

        InterviewMessage openingMessage = new InterviewMessage();
        openingMessage.setSession(session);
        openingMessage.setRole(ChatRole.ASSISTANT);
        openingMessage.setContent(opening.orElseGet(() -> fallbackOpening(problem)));
        interviewMessageRepository.save(openingMessage);

        return toResponse(session, List.of(openingMessage), opening.isPresent());
    }

    @Transactional
    public InterviewSessionResponse postMessage(User user, Long sessionId, InterviewMessageRequest request) {
        InterviewSession session = findOwned(user, sessionId);
        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This interview has already ended.");
        }
        String content = request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        List<InterviewMessage> history = interviewMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(session.getId());

        InterviewMessage userMessage = new InterviewMessage();
        userMessage.setSession(session);
        userMessage.setRole(ChatRole.USER);
        userMessage.setContent(content);
        interviewMessageRepository.save(userMessage);

        List<GeminiService.ChatTurn> turns = toTurns(history);
        turns.add(GeminiService.ChatTurn.user(content));
        Optional<String> reply = geminiService.generateChat(interviewerSystemInstruction(session.getProblem()), trimTurns(turns));

        if (reply.isPresent()) {
            InterviewMessage assistantMessage = new InterviewMessage();
            assistantMessage.setSession(session);
            assistantMessage.setRole(ChatRole.ASSISTANT);
            assistantMessage.setContent(reply.get());
            interviewMessageRepository.save(assistantMessage);
        }

        List<InterviewMessage> updated = interviewMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(session.getId());
        return toResponse(session, updated, reply.isPresent());
    }

    @Transactional
    public InterviewSessionResponse end(User user, Long sessionId) {
        InterviewSession session = findOwned(user, sessionId);
        List<InterviewMessage> history = interviewMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(session.getId());

        if (session.getStatus() == InterviewStatus.COMPLETED) {
            return toResponse(session, history, geminiService.isConfigured());
        }

        String transcript = history.stream()
                .map(m -> (m.getRole() == ChatRole.ASSISTANT ? "Interviewer: " : "Candidate: ") + m.getContent())
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                You just finished conducting a mock coding interview on this problem:

                Title: %s
                Difficulty: %s

                Full transcript:
                %s

                Give the candidate a final verdict in 4-5 short sentences: how they did overall, one \
                specific strength, one specific thing to improve, and whether you'd advance them to the \
                next round. No markdown headers.
                """.formatted(session.getProblem().getTitle(), session.getProblem().getDifficulty(), transcript);

        Optional<String> feedback = geminiService.generate(prompt);

        session.setStatus(InterviewStatus.COMPLETED);
        session.setEndedAt(Instant.now());
        session.setFeedback(feedback.orElse("Interview ended. (AI feedback isn't available right now.)"));
        session = interviewSessionRepository.save(session);

        return toResponse(session, history, feedback.isPresent());
    }

    @Transactional(readOnly = true)
    public InterviewSessionResponse get(User user, Long sessionId) {
        InterviewSession session = findOwned(user, sessionId);
        List<InterviewMessage> messages = interviewMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(session.getId());
        return toResponse(session, messages, geminiService.isConfigured());
    }

    /** List view only -- messages are omitted here (fetched via {@link #get}) to avoid an N+1 per session. */
    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> history(User user) {
        return interviewSessionRepository.findAllByUserIdOrderByStartedAtDesc(user.getId()).stream()
                .map(s -> toResponse(s, List.of(), geminiService.isConfigured()))
                .toList();
    }

    private Problem pickProblem(User user, StartInterviewRequest request) {
        if (request.problemId() != null) {
            return problemRepository.findById(request.problemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No problem with id " + request.problemId()));
        }

        Difficulty difficulty = parseDifficulty(request.difficulty());
        List<Problem> candidates = problemRepository.findAll().stream()
                .filter(p -> difficulty == null || p.getDifficulty() == difficulty)
                .toList();
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No problems match that difficulty.");
        }

        Set<Long> doneIds = userProgressRepository.findAllByUserIdAndStatus(user.getId(), Status.DONE).stream()
                .map(up -> up.getProblem().getId())
                .collect(Collectors.toSet());
        List<Problem> notDone = candidates.stream().filter(p -> !doneIds.contains(p.getId())).toList();
        List<Problem> pool = notDone.isEmpty() ? candidates : notDone;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private Difficulty parseDifficulty(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) return null;
        try {
            return Difficulty.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String interviewerSystemInstruction(Problem problem) {
        return """
                You are conducting a realistic mock coding interview. Stay in character as the interviewer \
                throughout -- never break the fourth wall or mention that you are an AI.

                The problem for this interview:
                Title: %s
                Difficulty: %s
                Statement: %s

                Rules: ask clarifying questions when it makes sense, react to the candidate's stated approach \
                before they code (push back gently on a suboptimal one rather than accepting it immediately), \
                and keep each of your turns short (2-4 sentences) like a real interviewer would, not a lecture. \
                Don't solve the problem for them.
                """.formatted(
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getStatement() != null ? problem.getStatement() : ""
        );
    }

    private String fallbackOpening(Problem problem) {
        return "Let's get started. Here's your problem: \"" + problem.getTitle() + "\" ("
                + problem.getDifficulty() + "). Take a moment to read it, then walk me through your initial approach.";
    }

    private List<GeminiService.ChatTurn> toTurns(List<InterviewMessage> messages) {
        List<GeminiService.ChatTurn> turns = new ArrayList<>();
        for (InterviewMessage m : messages) {
            turns.add(m.getRole() == ChatRole.USER
                    ? GeminiService.ChatTurn.user(m.getContent())
                    : GeminiService.ChatTurn.model(m.getContent()));
        }
        return turns;
    }

    private List<GeminiService.ChatTurn> trimTurns(List<GeminiService.ChatTurn> turns) {
        int start = Math.max(0, turns.size() - MAX_HISTORY_TURNS);
        return turns.subList(start, turns.size());
    }

    private InterviewSession findOwned(User user, Long sessionId) {
        return interviewSessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No interview session with id " + sessionId));
    }

    private InterviewSessionResponse toResponse(InterviewSession session, List<InterviewMessage> messages, boolean aiAvailable) {
        List<InterviewMessageResponse> messageResponses = messages.stream()
                .map(m -> new InterviewMessageResponse(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
        return new InterviewSessionResponse(
                session.getId(),
                session.getProblem().getId(),
                session.getProblem().getTitle(),
                session.getProblem().getDifficulty(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getStatus(),
                session.getFeedback(),
                aiAvailable,
                messageResponses
        );
    }
}
