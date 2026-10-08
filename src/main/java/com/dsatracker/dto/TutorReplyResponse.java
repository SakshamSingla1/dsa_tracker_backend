package com.dsatracker.dto;

/** `available=false` means GEMINI_API_KEY isn't configured -- `userMessage` is still persisted
 *  and returned, but `assistantMessage` is null and the FE shows a "not configured" state. */
public record TutorReplyResponse(boolean available, TutorMessageResponse userMessage, TutorMessageResponse assistantMessage) {
}
