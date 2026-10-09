package com.dsatracker.dto.admin;

import com.dsatracker.model.MetaConfig;

import java.time.Instant;

public record MetaConfigResponse(Long id, String context, String data, Instant updatedAt, Long updatedBy) {
    public static MetaConfigResponse from(MetaConfig c) {
        return new MetaConfigResponse(c.getId(), c.getContext(), c.getData(), c.getUpdatedAt(), c.getUpdatedBy());
    }
}
