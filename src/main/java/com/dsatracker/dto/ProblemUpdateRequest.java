package com.dsatracker.dto;

import com.dsatracker.model.Status;

/**
 * Partial update payload for a problem. Any field left null is left unchanged
 * on the entity, so the frontend can send just the one thing it's toggling
 * (e.g. only {"status": "DONE"}) without re-sending the whole problem.
 */
public record ProblemUpdateRequest(
        Status status,
        String notes,
        String externalUrl,
        Boolean bookmarked
) {
}
