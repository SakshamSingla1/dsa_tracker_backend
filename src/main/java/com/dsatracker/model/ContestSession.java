package com.dsatracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * A self-service, timed practice session: the user picks a size/difficulty/duration and
 * a fresh set of problems is drawn for them (see {@link ContestProblem}). There is no
 * admin-curated contest catalog -- every session is generated on demand for its owner.
 */
@Entity
public class ContestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant startedAt = Instant.now();

    private int durationMinutes;

    /** Null means "any difficulty" -- the filter used when the problem set was drawn. */
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    /** Null means "all sheets" -- the filter used when the problem set was drawn. */
    private String sheetSlug;

    @Enumerated(EnumType.STRING)
    private ContestStatus status = ContestStatus.IN_PROGRESS;

    private Instant finishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getSheetSlug() {
        return sheetSlug;
    }

    public void setSheetSlug(String sheetSlug) {
        this.sheetSlug = sheetSlug;
    }

    public ContestStatus getStatus() {
        return status;
    }

    public void setStatus(ContestStatus status) {
        this.status = status;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Instant getEndsAt() {
        return startedAt.plusSeconds(durationMinutes * 60L);
    }
}
