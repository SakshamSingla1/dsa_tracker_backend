package com.dsatracker.service;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort, language-agnostic estimate of a submission's time complexity from its source
 * text alone -- no execution, no AST. Always available (unlike {@link GeminiService}'s AI
 * review), so "Submit" always shows *something*, with AI commentary layered on top when
 * configured. Deliberately conservative in its wording: this is a heuristic, not a proof.
 */
@Service
public class ComplexityAnalyzerService {

    private static final Pattern LOOP_START = Pattern.compile("\\b(for|while)\\s*\\(");
    // Matches a function/method declaration's name just before its parameter list, across the
    // languages this app supports (Java/C++/Python/JS all share this "name(" shape at the def site).
    private static final Pattern FUNCTION_DEF = Pattern.compile(
            "\\b(?:def\\s+)?([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\([^;{}]*\\)\\s*(\\{|:)"
    );

    public record Result(int maxLoopDepth, boolean likelyRecursive, String estimate) {
    }

    public Result analyze(String code) {
        if (code == null || code.isBlank()) {
            return new Result(0, false, "No code to analyze.");
        }
        String stripped = stripStringsAndComments(code);
        int maxDepth = maxLoopNestingDepth(stripped);
        boolean recursive = detectsRecursion(stripped);
        return new Result(maxDepth, recursive, estimate(maxDepth, recursive));
    }

    /** Tracks brace depth and a stack of "this loop started at depth D" markers to find the
     *  deepest point any loop is nested inside another loop. Brace-depth based, so it's thrown
     *  off by brace-less single-statement loop bodies (`for (...) doThing();`) -- acceptable
     *  for a best-effort heuristic; most real submissions use braces. */
    private int maxLoopNestingDepth(String code) {
        Deque<Integer> loopStartDepths = new ArrayDeque<>();
        int braceDepth = 0;
        int maxConcurrentLoops = 0;
        Matcher loopMatcher = LOOP_START.matcher(code);
        int lastLoopMatchEnd = 0;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '{') {
                braceDepth++;
            } else if (c == '}') {
                braceDepth--;
                // >= (not >): a loop pushed at depth D closes as soon as we're back to D (the brace
                // that just closed *is* its own body) -- `>` left it on the stack forever, so two
                // sequential (non-nested) loops at the same level were miscounted as nested.
                while (!loopStartDepths.isEmpty() && loopStartDepths.peek() >= braceDepth) {
                    loopStartDepths.pop();
                }
            }
            if (i >= lastLoopMatchEnd && loopMatcher.find(i) && loopMatcher.start() == i) {
                loopStartDepths.push(braceDepth);
                maxConcurrentLoops = Math.max(maxConcurrentLoops, loopStartDepths.size());
                lastLoopMatchEnd = loopMatcher.end();
            }
        }
        return maxConcurrentLoops;
    }

    /** Best-effort: true if some function appears to call itself by name within its own body. */
    private boolean detectsRecursion(String code) {
        Matcher defMatcher = FUNCTION_DEF.matcher(code);
        while (defMatcher.find()) {
            String name = defMatcher.group(1);
            if (name == null || name.isBlank() || isControlKeyword(name)) continue;
            int bodyStart = defMatcher.end();
            int bodyEnd = findMatchingBodyEnd(code, bodyStart, defMatcher.group(2));
            if (bodyEnd <= bodyStart) continue;
            String body = code.substring(bodyStart, bodyEnd);
            if (Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\(").matcher(body).find()) {
                return true;
            }
        }
        return false;
    }

    /** For a `{`-delimited body, finds the matching close brace. For Python's `:`-delimited body
     *  (no braces), falls back to "rest of the code" -- good enough since we only need the body
     *  text to contain a self-call somewhere, not an exact boundary. */
    private int findMatchingBodyEnd(String code, int bodyStart, String opener) {
        if (!"{".equals(opener)) return code.length();
        int depth = 1;
        for (int i = bodyStart; i < code.length(); i++) {
            if (code.charAt(i) == '{') depth++;
            else if (code.charAt(i) == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return code.length();
    }

    private boolean isControlKeyword(String word) {
        return switch (word) {
            case "if", "for", "while", "switch", "catch" -> true;
            default -> false;
        };
    }

    private String estimate(int maxLoopDepth, boolean likelyRecursive) {
        String loopEstimate = switch (maxLoopDepth) {
            case 0 -> "O(1)";
            case 1 -> "O(n)";
            case 2 -> "O(n²)";
            case 3 -> "O(n³)";
            default -> "O(n^" + maxLoopDepth + ")";
        };
        if (!likelyRecursive) {
            return loopEstimate + " (heuristic estimate from loop nesting -- not a guarantee)";
        }
        if (maxLoopDepth == 0) {
            return "Likely O(2ⁿ) or O(n!) (recursive, no loops detected) -- heuristic estimate, depends on branching and memoization";
        }
        return loopEstimate + " from loops, plus recursion detected -- actual complexity may be higher depending on the recursion's branching factor";
    }

    /** Strips `"..."`, `'...'`, `// line`, and `/* block *&#47;` content so keywords inside strings/comments
     *  don't skew the loop/brace scan. Deliberately simple (no escape-sequence handling beyond `\"`/`\\`). */
    private String stripStringsAndComments(String code) {
        StringBuilder out = new StringBuilder(code.length());
        int i = 0;
        int n = code.length();
        while (i < n) {
            char c = code.charAt(i);
            if (c == '/' && i + 1 < n && code.charAt(i + 1) == '/') {
                while (i < n && code.charAt(i) != '\n') i++;
            } else if (c == '/' && i + 1 < n && code.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(code.charAt(i) == '*' && code.charAt(i + 1) == '/')) i++;
                i = Math.min(i + 2, n);
            } else if (c == '"' || c == '\'') {
                char quote = c;
                out.append(' ');
                i++;
                while (i < n && code.charAt(i) != quote) {
                    if (code.charAt(i) == '\\' && i + 1 < n) i++;
                    i++;
                }
                i++;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
