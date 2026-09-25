package com.dsatracker.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;

import java.util.ArrayList;
import java.util.List;

/**
 * A problem in the shared sheet. This is reference data everyone sees the
 * same version of -- title, statement, complexity, example, tags, and the
 * external link. Per-user state (done/revise, notes, completion date) lives
 * on {@link UserProgress} instead, keyed by (user, problem).
 */
@Entity
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Column(length = 1000)
    private String statement;

    /** Expected complexity of the optimal / commonly-taught solution, e.g. "O(n log n)". */
    private String timeComplexity;
    private String spaceComplexity;

    /** @deprecated kept for the original seed format; new code should read/write {@link #examples}. */
    @Column(length = 500)
    private String exampleInput;

    /** @deprecated kept for the original seed format; new code should read/write {@link #examples}. */
    @Column(length = 500)
    private String exampleOutput;

    /** Worked examples shown on the problem page, richer than the single legacy input/output pair. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_examples", joinColumns = @JoinColumn(name = "problem_id"))
    @OrderColumn(name = "example_order")
    private List<Example> examples = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_constraints", joinColumns = @JoinColumn(name = "problem_id"))
    @Column(name = "constraint_text", length = 500)
    @OrderColumn(name = "constraint_order")
    private List<String> constraints = new ArrayList<>();

    /** Progressive hints, revealed one at a time on request instead of spoiling the approach up front. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_hints", joinColumns = @JoinColumn(name = "problem_id"))
    @Column(name = "hint_text", length = 1000)
    @OrderColumn(name = "hint_order")
    private List<String> hints = new ArrayList<>();

    /** Link to a written editorial/solution, if one has been added. */
    private String editorialUrl;

    /** Link to a video walkthrough, if one has been added. */
    private String videoUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_tags", joinColumns = @JoinColumn(name = "problem_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    /** Where to actually solve it (LeetCode, GfG, ...). Null when no link has been added yet. Shared/global. */
    private String externalUrl;

    private int orderIndex;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    /** Sample + hidden test cases used by the judge for Run/Submit. */
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<TestCase> testCases = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getTimeComplexity() {
        return timeComplexity;
    }

    public void setTimeComplexity(String timeComplexity) {
        this.timeComplexity = timeComplexity;
    }

    public String getSpaceComplexity() {
        return spaceComplexity;
    }

    public void setSpaceComplexity(String spaceComplexity) {
        this.spaceComplexity = spaceComplexity;
    }

    public String getExampleInput() {
        return exampleInput;
    }

    public void setExampleInput(String exampleInput) {
        this.exampleInput = exampleInput;
    }

    public String getExampleOutput() {
        return exampleOutput;
    }

    public void setExampleOutput(String exampleOutput) {
        this.exampleOutput = exampleOutput;
    }

    public List<Example> getExamples() {
        return examples;
    }

    public void setExamples(List<Example> examples) {
        this.examples = examples;
    }

    public List<String> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }

    public List<String> getHints() {
        return hints;
    }

    public void setHints(List<String> hints) {
        this.hints = hints;
    }

    public String getEditorialUrl() {
        return editorialUrl;
    }

    public void setEditorialUrl(String editorialUrl) {
        this.editorialUrl = editorialUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCase> testCases) {
        this.testCases = testCases;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }
}
