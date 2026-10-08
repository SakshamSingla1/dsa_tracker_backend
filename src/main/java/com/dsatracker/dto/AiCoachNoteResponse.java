package com.dsatracker.dto;

/** `available=false` means GEMINI_API_KEY isn't configured -- the FE shows a "not configured" state, not an error. */
public record AiCoachNoteResponse(boolean available, String note) {
}
