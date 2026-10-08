package com.dsatracker.dto;

/** `problemId` picks a specific problem; omit (null) for a random one, optionally narrowed by
 *  `difficulty` ("EASY"/"MEDIUM"/"HARD", or null/blank for any). */
public record StartInterviewRequest(Long problemId, String difficulty) {
}
