package com.dsatracker.model;

/** The outcome of judging a submission against a problem's test cases. */
public enum Verdict {
    ACCEPTED,
    WRONG_ANSWER,
    RUNTIME_ERROR,
    COMPILE_ERROR,
    TIME_LIMIT_EXCEEDED
}
