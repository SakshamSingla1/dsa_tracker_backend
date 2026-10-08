package com.dsatracker.service;

import com.dsatracker.model.Language;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComplexityAnalyzerServiceTest {

    private final ComplexityAnalyzerService service = new ComplexityAnalyzerService();

    @Test
    void noLoopsIsConstant() {
        var result = service.analyze("public class Main { public static void main(String[] a) { System.out.println(1); } }", Language.JAVA);
        assertThat(result.maxLoopDepth()).isZero();
        assertThat(result.estimate()).contains("O(1)");
    }

    @Test
    void singleLoopIsLinear() {
        var result = service.analyze("""
                public class Main {
                  public static void main(String[] a) {
                    for (int i = 0; i < 10; i++) { System.out.println(i); }
                  }
                }
                """, Language.JAVA);
        assertThat(result.maxLoopDepth()).isEqualTo(1);
        assertThat(result.estimate()).contains("O(n)").doesNotContain("O(n²)");
    }

    /** Regression: two sequential (not nested) loops at the same level were previously
     *  miscounted as nested (-> O(n^2)) because a closed loop's marker wasn't popped off the
     *  nesting stack until the surrounding block closed too, not when the loop's own body did. */
    @Test
    void sequentialLoopsAreNotNested() {
        var result = service.analyze("""
                public class Main {
                  public static void main(String[] a) {
                    for (int i = 0; i < 10; i++) { System.out.println(i); }
                    for (int j = 0; j < 10; j++) { System.out.println(j); }
                  }
                }
                """, Language.JAVA);
        assertThat(result.maxLoopDepth()).isEqualTo(1);
        assertThat(result.estimate()).contains("O(n)").doesNotContain("O(n²)");
    }

    @Test
    void nestedLoopsAreQuadratic() {
        var result = service.analyze("""
                public class Main {
                  public static void main(String[] a) {
                    for (int i = 0; i < 10; i++) {
                      for (int j = 0; j < 10; j++) { System.out.println(i + j); }
                    }
                  }
                }
                """, Language.JAVA);
        assertThat(result.maxLoopDepth()).isEqualTo(2);
        assertThat(result.estimate()).contains("O(n²)");
    }

    @Test
    void detectsSelfRecursion() {
        var result = service.analyze("""
                public class Main {
                  static int fib(int n) {
                    if (n <= 1) return n;
                    return fib(n - 1) + fib(n - 2);
                  }
                }
                """, Language.JAVA);
        assertThat(result.likelyRecursive()).isTrue();
    }

    /** Regression: Python has no `(` around loop conditions, so the brace-based detector used for
     *  Java/C++/JS always found zero loops in Python source. */
    @Test
    void pythonNestedLoopsAreQuadratic() {
        var result = service.analyze("""
                def pairs(items):
                    result = []
                    for i in items:
                        for j in items:
                            result.append((i, j))
                    return result
                """, Language.PYTHON);
        assertThat(result.maxLoopDepth()).isEqualTo(2);
        assertThat(result.estimate()).contains("O(n²)");
    }

    @Test
    void pythonSequentialLoopsAreNotNested() {
        var result = service.analyze("""
                def run(items):
                    for i in items:
                        print(i)
                    for j in items:
                        print(j)
                """, Language.PYTHON);
        assertThat(result.maxLoopDepth()).isEqualTo(1);
    }

    @Test
    void pythonDetectsSelfRecursion() {
        var result = service.analyze("""
                def fib(n):
                    if n <= 1:
                        return n
                    return fib(n - 1) + fib(n - 2)
                """, Language.PYTHON);
        assertThat(result.likelyRecursive()).isTrue();
    }

    /** Regression: a Python function's "body" was previously treated as the rest of the whole
     *  file (no closing brace to bound it), so a driver line invoking the function *after* its
     *  definition -- e.g. the judge harness calling the submitted function -- was misread as the
     *  function calling itself. */
    @Test
    void pythonTrailingDriverCallIsNotRecursion() {
        var result = service.analyze("""
                def square(n):
                    return n * n

                print(square(5))
                """, Language.PYTHON);
        assertThat(result.likelyRecursive()).isFalse();
    }
}
