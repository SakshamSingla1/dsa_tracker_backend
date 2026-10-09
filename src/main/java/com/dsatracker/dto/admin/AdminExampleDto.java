package com.dsatracker.dto.admin;

import com.dsatracker.model.Example;

public record AdminExampleDto(String input, String output, String explanation) {
    public static AdminExampleDto from(Example e) {
        return new AdminExampleDto(e.getInput(), e.getOutput(), e.getExplanation());
    }

    public Example toEntity() {
        return new Example(input, output, explanation);
    }
}
