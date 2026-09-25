package com.dsatracker.dto;

import com.dsatracker.model.Example;

public record ExampleResponse(String input, String output, String explanation) {
    public static ExampleResponse from(Example e) {
        return new ExampleResponse(e.getInput(), e.getOutput(), e.getExplanation());
    }
}
