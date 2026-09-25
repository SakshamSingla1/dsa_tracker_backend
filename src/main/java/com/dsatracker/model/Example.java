package com.dsatracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One worked example shown on the problem page: input, output, and why. */
@Embeddable
public class Example {

    @Column(length = 1000)
    private String input;

    @Column(length = 1000)
    private String output;

    @Column(length = 1000)
    private String explanation;

    public Example() {
    }

    public Example(String input, String output, String explanation) {
        this.input = input;
        this.output = output;
        this.explanation = explanation;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
