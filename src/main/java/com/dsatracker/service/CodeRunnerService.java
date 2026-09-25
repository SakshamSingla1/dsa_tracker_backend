package com.dsatracker.service;

import com.dsatracker.dto.RunRequest;
import com.dsatracker.dto.RunResponse;
import com.dsatracker.model.Language;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles (where applicable) and runs source code the user submits from the
 * code editor, in one of a few supported languages. This is a local,
 * single-user practice tool -- it shells out to the system's own toolchains
 * with a timeout and a fresh temp directory per run, which is a reasonable
 * tradeoff for "run my own code on my own machine" but is NOT a sandbox:
 * never expose this endpoint to untrusted callers or the public internet.
 */
@Service
public class CodeRunnerService {

    private static final Pattern PUBLIC_CLASS = Pattern.compile("public\\s+(?:final\\s+)?class\\s+(\\w+)");
    private static final Duration COMPILE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration RUN_TIMEOUT = Duration.ofSeconds(8);
    private static final int MAX_OUTPUT_CHARS = 20_000;

    /** One test case's raw execution result, before it's compared against an expected output. */
    public record CaseRun(String stdout, String stderr, Integer exitCode, boolean timedOut, long durationMs) {
    }

    /** The result of compiling (once) and then running the same program against several stdins. */
    public record BatchRunResult(boolean compiled, String compileError, List<CaseRun> cases) {
        public static BatchRunResult compileFailure(String error) {
            return new BatchRunResult(false, error, List.of());
        }
    }

    public RunResponse run(RunRequest request) {
        String code = request.code() == null ? "" : request.code();
        Language language = request.language() == null ? Language.JAVA : request.language();
        BatchRunResult batch = runBatch(language, code, List.of(request.stdin() == null ? "" : request.stdin()));

        if (!batch.compiled()) {
            return new RunResponse(false, false, "", batch.compileError(), null, 0);
        }
        CaseRun only = batch.cases().get(0);
        if (only.timedOut()) {
            return new RunResponse(true, true, only.stdout(), only.stderr(), null, only.durationMs());
        }
        return new RunResponse(true, false, only.stdout(), only.stderr(), only.exitCode(), only.durationMs());
    }

    /**
     * Compiles {@code code} once (for compiled languages) and then runs it once per
     * entry in {@code stdins}, in order. Used by the judge so a submission with many
     * test cases doesn't pay the compile cost repeatedly.
     */
    public BatchRunResult runBatch(Language language, String code, List<String> stdins) {
        code = code == null ? "" : code;

        Path workDir;
        try {
            workDir = Files.createTempDirectory("dsa-run-");
        } catch (IOException e) {
            return BatchRunResult.compileFailure("Could not create a working directory: " + e.getMessage());
        }

        try {
            return switch (language) {
                case JAVA -> batchJava(workDir, code, stdins);
                case CPP -> batchCpp(workDir, code, stdins);
                case PYTHON -> batchInterpreted(workDir, code, stdins, "main.py", "python3", "main.py");
                case JAVASCRIPT -> batchInterpreted(workDir, code, stdins, "main.js", "node", "main.js");
            };
        } catch (IOException e) {
            return BatchRunResult.compileFailure("Failed to run: " + e.getMessage());
        } finally {
            deleteRecursively(workDir);
        }
    }

    private BatchRunResult batchJava(Path workDir, String code, List<String> stdins) throws IOException {
        String className = extractClassName(code);
        Path sourceFile = workDir.resolve(className + ".java");
        Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

        ExecResult compile = exec(workDir, COMPILE_TIMEOUT, "javac", "-encoding", "UTF-8", sourceFile.getFileName().toString());
        if (compile.timedOut) {
            return BatchRunResult.compileFailure("Compilation timed out.");
        }
        if (compile.exitCode != 0) {
            return BatchRunResult.compileFailure(compile.stderr);
        }

        List<CaseRun> cases = new ArrayList<>();
        for (String stdin : stdins) {
            long start = System.currentTimeMillis();
            ExecResult run = execWithInput(workDir, RUN_TIMEOUT, writeStdin(workDir, stdin), "java", "-cp", workDir.toString(), className);
            cases.add(toCaseRun(run, start));
        }
        return new BatchRunResult(true, null, cases);
    }

    private BatchRunResult batchCpp(Path workDir, String code, List<String> stdins) throws IOException {
        Path sourceFile = workDir.resolve("main.cpp");
        Files.writeString(sourceFile, code, StandardCharsets.UTF_8);
        String binaryName = "main.out";

        ExecResult compile = exec(workDir, COMPILE_TIMEOUT, "g++", "-O2", "-std=c++17", "-o", binaryName, "main.cpp");
        if (compile.timedOut) {
            return BatchRunResult.compileFailure("Compilation timed out.");
        }
        if (compile.exitCode != 0) {
            return BatchRunResult.compileFailure(compile.stderr);
        }

        List<CaseRun> cases = new ArrayList<>();
        for (String stdin : stdins) {
            long start = System.currentTimeMillis();
            ExecResult run = execWithInput(workDir, RUN_TIMEOUT, writeStdin(workDir, stdin), workDir.resolve(binaryName).toString());
            cases.add(toCaseRun(run, start));
        }
        return new BatchRunResult(true, null, cases);
    }

    private BatchRunResult batchInterpreted(Path workDir, String code, List<String> stdins, String fileName, String... interpreterAndFile) throws IOException {
        Path sourceFile = workDir.resolve(fileName);
        Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

        List<CaseRun> cases = new ArrayList<>();
        for (String stdin : stdins) {
            long start = System.currentTimeMillis();
            ExecResult run = execWithInput(workDir, RUN_TIMEOUT, writeStdin(workDir, stdin), interpreterAndFile);
            cases.add(toCaseRun(run, start));
        }
        return new BatchRunResult(true, null, cases);
    }

    private CaseRun toCaseRun(ExecResult run, long start) {
        long duration = System.currentTimeMillis() - start;
        if (run.timedOut) {
            return new CaseRun(run.stdout, run.stderr, null, true, duration);
        }
        return new CaseRun(run.stdout, run.stderr, run.exitCode, false, duration);
    }

    private Path writeStdin(Path workDir, String stdin) throws IOException {
        // Each case gets its own stdin file so a slow/failed case can't leak input into the next one.
        Path stdinFile = Files.createTempFile(workDir, "stdin-", ".txt");
        Files.writeString(stdinFile, stdin == null ? "" : stdin, StandardCharsets.UTF_8);
        return stdinFile;
    }

    private String extractClassName(String code) {
        Matcher m = PUBLIC_CLASS.matcher(code);
        return m.find() ? m.group(1) : "Main";
    }

    private record ExecResult(int exitCode, String stdout, String stderr, boolean timedOut) {
    }

    private ExecResult exec(Path workDir, Duration timeout, String... command) throws IOException {
        return execWithInput(workDir, timeout, null, command);
    }

    private ExecResult execWithInput(Path workDir, Duration timeout, Path stdinFile, String... command) throws IOException {
        Path outFile = Files.createTempFile(workDir, "out-", ".txt");
        Path errFile = Files.createTempFile(workDir, "err-", ".txt");

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectOutput(outFile.toFile())
                .redirectError(errFile.toFile());
        if (stdinFile != null) {
            pb.redirectInput(stdinFile.toFile());
        }

        Process process = pb.start();
        boolean finished = false;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!finished) {
            process.destroyForcibly();
            return new ExecResult(-1, truncate(readQuietly(outFile)), truncate(readQuietly(errFile)), true);
        }

        return new ExecResult(process.exitValue(), truncate(readQuietly(outFile)), truncate(readQuietly(errFile)), false);
    }

    private String readQuietly(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private String truncate(String s) {
        if (s.length() <= MAX_OUTPUT_CHARS) return s;
        return s.substring(0, MAX_OUTPUT_CHARS) + "\n... (output truncated)";
    }

    private void deleteRecursively(Path dir) {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
