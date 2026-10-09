package com.dsatracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * A generic admin-editable key/value settings row: {@code context} names the setting (e.g.
 * "ANNOUNCEMENT_BANNER"), {@code data} is an opaque JSON string the backend never parses --
 * the admin UI and whatever eventually reads a given context own its shape between them.
 */
@Entity
@Table(name = "meta_config", uniqueConstraints = @UniqueConstraint(columnNames = "context"))
public class MetaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String context;

    @Lob
    @Column(nullable = false)
    private String data;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    /** Id of the admin who last saved this row -- a minimal breadcrumb, not a full audit log. */
    private Long updatedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
