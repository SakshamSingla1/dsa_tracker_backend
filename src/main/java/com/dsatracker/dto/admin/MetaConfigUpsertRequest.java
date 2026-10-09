package com.dsatracker.dto.admin;

/** Raw JSON string, opaque to the backend -- the admin UI owns validating/shaping it. */
public record MetaConfigUpsertRequest(String data) {
}
