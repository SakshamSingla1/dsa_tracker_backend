package com.dsatracker.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComplexityAnalyzerServiceTest {

    private final ComplexityAnalyzerService service = new ComplexityAnalyzerService();

    @Test
    void noLoopsIsConstant() {
        var result = service.analyze("public class Main { public static void main(String[] a) { System.out.println(1); } }");
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
                """);
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
                """);
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
                """);
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
                """);
        assertThat(result.likelyRecursive()).isTrue();
    }
}
