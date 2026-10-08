package com.dsatracker.service;

import com.dsatracker.model.Language;
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
    // brace-delimited languages this app supports (Java/C++/JS all share this "name(" shape at
    // the def site). Python is handled separately -- see the *Python methods below -- since it
    // has neither parens around loop conditions nor braces around block bodies.
    private static final Pattern FUNCTION_DEF = Pattern.compile(
            "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\([^;{}]*\\)\\s*(\\{)"
    );
    private static final Pattern PYTHON_LOOP_HEADER = Pattern.compile("^(\\s*)(for|while)\\b.*:\\s*$");
    private static final Pattern PYTHON_DEF_HEADER = Pattern.compile("^(\\s*)def\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");

    public record Result(int maxLoopDepth, boolean likelyRecursive, String estimate) {
    }

    public Result analyze(String code, Language language) {
        if (code == null || code.isBlank()) {
            return new Result(0, false, "No code to analyze.");
        }
        String stripped = stripStringsAndComments(code);
        int maxDepth;
        boolean recursive;
        if (language == Language.PYTHON) {
            // Python has no `(` around loop conditions and no `{}` around block bodies -- both
            // brace-depth-based methods below silently found "no loops" and could misattribute a
            // trailing call (e.g. the test harness invoking the submitted function) as recursion.
            maxDepth = maxLoopNestingDepthPython(stripped);
            recursive = detectsRecursionPython(stripped);
        } else {
            maxDepth = maxLoopNestingDepth(stripped);
            recursive = detectsRecursion(stripped);
        }
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
            int bodyEnd = findMatchingBodyEnd(code, bodyStart);
            if (bodyEnd <= bodyStart) continue;
            String body = code.substring(bodyStart, bodyEnd);
            if (Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\(").matcher(body).find()) {
                return true;
            }
        }
        return false;
    }

    /** Finds the close brace matching the one just opened at `bodyStart - 1`. */
    private int findMatchingBodyEnd(String code, int bodyStart) {
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

    /** Python has no braces -- blocks are delimited by indentation, so loop nesting is tracked
     *  by each loop header's own indentation instead of brace depth. A loop's body must be
     *  strictly more indented than its header, so a loop closes as soon as a later non-blank
     *  line's indentation drops back to (or below) its header's. */
    private int maxLoopNestingDepthPython(String code) {
        Deque<Integer> loopHeaderIndents = new ArrayDeque<>();
        int maxConcurrentLoops = 0;
        for (String line : code.split("\n", -1)) {
            if (line.isBlank()) continue;
            int indent = indentWidth(line);
            while (!loopHeaderIndents.isEmpty() && indent <= loopHeaderIndents.peek()) {
                loopHeaderIndents.pop();
            }
            if (PYTHON_LOOP_HEADER.matcher(line).matches()) {
                loopHeaderIndents.push(indent);
                maxConcurrentLoops = Math.max(maxConcurrentLoops, loopHeaderIndents.size());
            }
        }
        return maxConcurrentLoops;
    }

    /** Same self-call check as {@link #detectsRecursion}, but a function's body is bounded by
     *  indentation (every line more indented than its own `def`) instead of a matching `}` --
     *  without this, a trailing call to the function elsewhere in the submitted source (e.g. the
     *  judge's own driver code invoking it) would be misread as the function calling itself. */
    private boolean detectsRecursionPython(String code) {
        String[] lines = code.split("\n", -1);
        for (int start = 0; start < lines.length; start++) {
            Matcher defMatcher = PYTHON_DEF_HEADER.matcher(lines[start]);
            if (!defMatcher.find()) continue;
            int defIndent = defMatcher.group(1).length();
            String name = defMatcher.group(2);

            StringBuilder body = new StringBuilder();
            for (int j = start + 1; j < lines.length; j++) {
                String line = lines[j];
                if (!line.isBlank() && indentWidth(line) <= defIndent) break;
                body.append(line).append('\n');
            }
            if (Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\(").matcher(body).find()) {
                return true;
            }
        }
        return false;
    }

    /** Leading-whitespace width of a line, tabs counted as 8 columns (Python's own default). */
    private int indentWidth(String line) {
        int width = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') width++;
            else if (c == '\t') width += 8;
            else break;
        }
        return width;
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

    /** Strips `"..."`, `'...'`, `// line`, `# line` (Python), and `/* block *&#47;` content so
     *  keywords inside strings/comments don't skew the loop/brace scan. Deliberately simple (no
     *  escape-sequence handling beyond `\"`/`\\`, and no triple-quoted-string awareness). */
    private String stripStringsAndComments(String code) {
        StringBuilder out = new StringBuilder(code.length());
        int i = 0;
        int n = code.length();
        while (i < n) {
            char c = code.charAt(i);
            if (c == '#') {
                while (i < n && code.charAt(i) != '\n') i++;
            } else if (c == '/' && i + 1 < n && code.charAt(i + 1) == '/') {
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
