package com.dsatracker.controller;

import com.dsatracker.dto.AiCoachNoteResponse;
import com.dsatracker.dto.AiHintRequest;
import com.dsatracker.dto.AiHintResponse;
import com.dsatracker.dto.AiReviewRequest;
import com.dsatracker.dto.AiReviewResponse;
import com.dsatracker.dto.TutorMessageRequest;
import com.dsatracker.dto.TutorMessageResponse;
import com.dsatracker.dto.TutorReplyResponse;
import com.dsatracker.model.User;
import com.dsatracker.service.AiService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /** One contextual, non-spoiling hint from Gemini. `available=false` when GEMINI_API_KEY isn't set. */
    @PostMapping("/problems/{id}/ai-hint")
    public AiHintResponse getHint(@PathVariable Long id, @RequestBody(required = false) AiHintRequest request) {
        return aiService.getHint(id, request != null ? request : new AiHintRequest(null, null));
    }

    /** Short AI code review of a just-submitted solution. Not persisted. */
    @PostMapping("/ai-review")
    public AiReviewResponse getReview(@RequestBody AiReviewRequest request) {
        return aiService.getReview(request);
    }

    /** Personalized "what to focus on" note built from the user's own practice stats. */
    @GetMapping("/analytics/ai-coach-note")
    public AiCoachNoteResponse getCoachNote(@AuthenticationPrincipal User user) {
        return aiService.getCoachNote(user.getId());
    }

    /** Full tutor-chat history for this user + problem, oldest first. */
    @GetMapping("/problems/{id}/tutor/messages")
    public List<TutorMessageResponse> getTutorMessages(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return aiService.getTutorHistory(user.getId(), id);
    }

    /** Sends one message to the AI tutor for this problem and gets a reply, both persisted. */
    @PostMapping("/problems/{id}/tutor/messages")
    public TutorReplyResponse sendTutorMessage(
            @AuthenticationPrincipal User user, @PathVariable Long id, @RequestBody TutorMessageRequest request
    ) {
        return aiService.sendTutorMessage(user, id, request);
    }

    /** Clears the tutor-chat history for this user + problem, starting fresh. */
    @DeleteMapping("/problems/{id}/tutor/messages")
    public void clearTutorMessages(@AuthenticationPrincipal User user, @PathVariable Long id) {
        aiService.clearTutorHistory(user.getId(), id);
    }
}
