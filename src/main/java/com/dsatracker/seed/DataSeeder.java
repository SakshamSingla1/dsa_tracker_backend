package com.dsatracker.seed;

import com.dsatracker.model.Difficulty;
import com.dsatracker.model.Example;
import com.dsatracker.model.Problem;
import com.dsatracker.model.Sheet;
import com.dsatracker.model.TestCase;
import com.dsatracker.model.Topic;
import com.dsatracker.repository.SheetRepository;
import com.dsatracker.repository.TopicRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Seeds every curated problem sheet from src/main/resources/data/*.json on
 * first run. Only runs a full seed when the sheet table is empty, so it never
 * overwrites progress you've already made. On every later run it instead does
 * a reconciliation pass -- see {@link #reconcile()} -- so sheets/topics/test
 * cases added to the JSON files after the initial seed still reach an
 * already-seeded database without wiping anyone's progress.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final SheetRepository sheetRepository;
    private final TopicRepository topicRepository;
    private final ObjectMapper objectMapper;

    public DataSeeder(SheetRepository sheetRepository, TopicRepository topicRepository, ObjectMapper objectMapper) {
        this.sheetRepository = sheetRepository;
        this.topicRepository = topicRepository;
        this.objectMapper = objectMapper;
    }

    /** Which sheets to seed, and where their data lives. Add a row here + a JSON file to add a new sheet. */
    private record SheetDef(String slug, String name, String description, String resourcePath) {
    }

    private static final List<SheetDef> SHEETS = List.of(
            new SheetDef(
                    "a2z",
                    "Striver's A2Z Sheet",
                    "A structured, topic-by-topic path through DSA fundamentals to advanced patterns.",
                    "data/a2z-sheet.json"
            ),
            new SheetDef(
                    "blind75",
                    "Blind 75",
                    "The classic 75-problem interview list, grouped by pattern rather than topic depth.",
                    "data/blind75-sheet.json"
            ),
            new SheetDef(
                    "patterns",
                    "Coding Patterns",
                    "Interview problems grouped by the reusable technique that solves them -- sliding window, two pointers, topological sort, and more.",
                    "data/patterns-sheet.json"
            )
    );

    private record SeedExample(String input, String output, String explanation) {
    }

    private record SeedTestCase(String input, String expectedOutput, Boolean sample) {
    }

    private record SeedProblem(
            String title, String difficulty, List<String> tags, String externalUrl, String statement,
            String timeComplexity, String spaceComplexity, String exampleInput, String exampleOutput,
            List<SeedExample> examples, List<String> constraints, List<String> hints,
            String editorialUrl, String videoUrl, List<SeedTestCase> testCases
    ) {
    }

    private record SeedTopic(String name, List<SeedProblem> problems) {
    }

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        if (sheetRepository.count() > 0) {
            reconcile();
            return;
        }

        int sheetOrder = 0;
        for (SheetDef sheetDef : SHEETS) {
            Sheet sheet = new Sheet();
            sheet.setSlug(sheetDef.slug());
            sheet.setName(sheetDef.name());
            sheet.setDescription(sheetDef.description());
            sheet.setOrderIndex(sheetOrder++);
            sheet = sheetRepository.save(sheet);
            seedSheet(sheet, sheetDef.resourcePath());
        }
    }

    /**
     * Runs on every startup after the first. Re-reads the seed JSON and, without ever touching
     * UserProgress/Submission, fills in whatever this DB is missing relative to it:
     * <ul>
     *   <li>a sheet in {@link #SHEETS} that doesn't exist yet in the DB is fully seeded
     *       (appended after whatever sheets already exist);</li>
     *   <li>a topic in an existing sheet's JSON that doesn't exist yet in the DB is fully
     *       seeded (appended after that sheet's existing topics);</li>
     *   <li>a problem that already exists but currently has zero test cases gets them
     *       backfilled (and its statement refreshed, in case it grew an "Input: ..." line
     *       documenting the format those new test cases rely on) if the JSON now provides some.</li>
     * </ul>
     * This only ever fills gaps -- it never overwrites or removes anything a user could have
     * touched.
     */
    private void reconcile() throws IOException {
        int nextSheetOrder = (int) sheetRepository.count();

        for (SheetDef sheetDef : SHEETS) {
            Sheet sheet = sheetRepository.findBySlug(sheetDef.slug()).orElse(null);
            if (sheet == null) {
                sheet = new Sheet();
                sheet.setSlug(sheetDef.slug());
                sheet.setName(sheetDef.name());
                sheet.setDescription(sheetDef.description());
                sheet.setOrderIndex(nextSheetOrder++);
                sheet = sheetRepository.save(sheet);
                seedSheet(sheet, sheetDef.resourcePath());
                continue; // brand new sheet -- every topic/problem in it is new, nothing to reconcile
            }

            List<SeedTopic> seedTopics;
            try (InputStream in = new ClassPathResource(sheetDef.resourcePath()).getInputStream()) {
                seedTopics = objectMapper.readValue(in, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, SeedTopic.class));
            }

            List<Topic> existingTopics = topicRepository.findAllBySheetSlugOrderByOrderIndexAsc(sheetDef.slug());
            Map<String, Topic> topicsByName = existingTopics.stream()
                    .collect(Collectors.toMap(Topic::getName, Function.identity(), (a, b) -> a));
            int nextTopicOrder = existingTopics.size();

            for (SeedTopic seedTopic : seedTopics) {
                Topic topic = topicsByName.get(seedTopic.name());
                if (topic == null) {
                    // A whole new topic added to this sheet since the initial seed -- seed it in full.
                    Topic newTopic = new Topic();
                    newTopic.setName(seedTopic.name());
                    newTopic.setOrderIndex(nextTopicOrder++);
                    newTopic.setSheet(sheet);
                    int problemOrder = 0;
                    for (SeedProblem seedProblem : seedTopic.problems()) {
                        newTopic.getProblems().add(buildProblem(seedProblem, newTopic, problemOrder++));
                    }
                    topicRepository.save(newTopic);
                    continue;
                }

                Map<String, Problem> problemsByTitle = topic.getProblems().stream()
                        .collect(Collectors.toMap(Problem::getTitle, Function.identity(), (a, b) -> a));

                for (SeedProblem seedProblem : seedTopic.problems()) {
                    Problem problem = problemsByTitle.get(seedProblem.title());
                    if (problem == null || !problem.getTestCases().isEmpty()) {
                        continue;
                    }
                    if (seedProblem.testCases() == null || seedProblem.testCases().isEmpty()) {
                        continue;
                    }

                    problem.setStatement(seedProblem.statement());

                    int testOrder = 0;
                    for (SeedTestCase st : seedProblem.testCases()) {
                        TestCase testCase = new TestCase();
                        testCase.setInput(st.input());
                        testCase.setExpectedOutput(st.expectedOutput());
                        testCase.setSample(st.sample() == null || st.sample());
                        testCase.setOrderIndex(testOrder++);
                        testCase.setProblem(problem);
                        problem.getTestCases().add(testCase);
                    }
                }
            }
        }
    }

    private void seedSheet(Sheet sheet, String resourcePath) throws IOException {
        List<SeedTopic> topics;
        try (InputStream in = new ClassPathResource(resourcePath).getInputStream()) {
            topics = objectMapper.readValue(in, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, SeedTopic.class));
        }

        int topicOrder = 0;
        for (SeedTopic seedTopic : topics) {
            Topic topic = new Topic();
            topic.setName(seedTopic.name());
            topic.setOrderIndex(topicOrder++);
            topic.setSheet(sheet);

            int problemOrder = 0;
            for (SeedProblem seedProblem : seedTopic.problems()) {
                topic.getProblems().add(buildProblem(seedProblem, topic, problemOrder++));
            }

            topicRepository.save(topic);
        }
    }

    private Problem buildProblem(SeedProblem seedProblem, Topic topic, int orderIndex) {
        Problem problem = new Problem();
        problem.setTitle(seedProblem.title());
        problem.setDifficulty(Difficulty.valueOf(seedProblem.difficulty()));
        problem.setStatement(seedProblem.statement());
        problem.setTimeComplexity(seedProblem.timeComplexity());
        problem.setSpaceComplexity(seedProblem.spaceComplexity());
        problem.setExampleInput(seedProblem.exampleInput());
        problem.setExampleOutput(seedProblem.exampleOutput());
        problem.setTags(seedProblem.tags());
        problem.setExternalUrl(seedProblem.externalUrl());
        problem.setConstraints(seedProblem.constraints() != null ? seedProblem.constraints() : List.of());
        problem.setHints(seedProblem.hints() != null ? seedProblem.hints() : List.of());
        problem.setEditorialUrl(seedProblem.editorialUrl());
        problem.setVideoUrl(seedProblem.videoUrl());
        problem.setOrderIndex(orderIndex);
        problem.setTopic(topic);

        // Richer examples, if given; otherwise fall back to the single legacy input/output pair.
        if (seedProblem.examples() != null && !seedProblem.examples().isEmpty()) {
            problem.setExamples(seedProblem.examples().stream()
                    .map(e -> new Example(e.input(), e.output(), e.explanation()))
                    .toList());
        } else if (seedProblem.exampleInput() != null && seedProblem.exampleOutput() != null) {
            problem.setExamples(List.of(new Example(seedProblem.exampleInput(), seedProblem.exampleOutput(), null)));
        }

        // Only explicit, hand-verified test cases power Run/Submit. exampleInput/exampleOutput
        // (and examples[]) are display strings like "n = 1234" or "nums = [2,7], target = 9" --
        // NOT valid raw stdin -- so they must never be auto-turned into a judged test case.
        int testOrder = 0;
        if (seedProblem.testCases() != null) {
            for (SeedTestCase st : seedProblem.testCases()) {
                TestCase testCase = new TestCase();
                testCase.setInput(st.input());
                testCase.setExpectedOutput(st.expectedOutput());
                testCase.setSample(st.sample() == null || st.sample());
                testCase.setOrderIndex(testOrder++);
                testCase.setProblem(problem);
                problem.getTestCases().add(testCase);
            }
        }

        return problem;
    }
}
