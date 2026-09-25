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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/**
 * One user's private state for one problem: status, notes, and the day it
 * was last marked DONE. A row is created lazily the first time a user
 * touches a problem; until then they're implicitly TODO with no notes.
 */
@Entity
@Table(name = "user_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "problem_id"}))
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    private Status status = Status.TODO;

    @Column(length = 2000)
    private String notes;

    /** The day this was last marked DONE. Cleared when the status moves away from DONE. Powers the streak calendar. */
    private LocalDate completedAt;

    /** A personal watchlist flag, independent of status -- like starring a problem on LeetCode. */
    private boolean bookmarked;

    /** Next day this problem should resurface for review. Set on entering REVISE, cleared on leaving it. */
    private LocalDate reviewDueAt;

    /** Index into the Leitner interval ladder (see ProblemService.REVIEW_INTERVALS). Resets to 0 on a fresh REVISE or a "forgot". */
    private int reviewStage;

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

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDate completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public LocalDate getReviewDueAt() {
        return reviewDueAt;
    }

    public void setReviewDueAt(LocalDate reviewDueAt) {
        this.reviewDueAt = reviewDueAt;
    }

    public int getReviewStage() {
        return reviewStage;
    }

    public void setReviewStage(int reviewStage) {
        this.reviewStage = reviewStage;
    }
}
