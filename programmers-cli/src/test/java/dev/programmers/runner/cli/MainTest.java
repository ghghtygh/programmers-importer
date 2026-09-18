package dev.programmers.runner.cli;

import dev.programmers.runner.core.fetch.*;
import dev.programmers.runner.core.error.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @TempDir Path directory;
    private final StringWriter stdout = new StringWriter();
    private final StringWriter stderr = new StringWriter();
    private static final String URL = "https://school.programmers.co.kr/learn/courses/30/lessons/1";
    private final ProblemFetcher fixture = url -> new RawProblem(url, """
            <title>테스트 문제</title>
            <textarea>class Solution {public int solution(int x){return 0;}}</textarea>
            <h5>입출력 예</h5><table><tr><th>x</th><th>result</th></tr>
            <tr><td>1</td><td>1</td></tr><tr><td>2</td><td>2</td></tr></table>
            """);

    private int run(String... args) { return Main.run(args, new PrintWriter(stdout), new PrintWriter(stderr), fixture); }

    @Test
    void importsThenProtectsFilesAndExplicitlyOverwrites() throws Exception {
        String output = directory.resolve("공백 있는 경로").toString();
        assertEquals(0, run("import", URL, "--output", output));
        Path solution = Path.of(output, "src/main/java/programmers/p1/Solution.java");
        assertTrue(Files.exists(solution));
        assertTrue(stdout.toString().contains("공개 예제 2개"));
        assertEquals("", stderr.toString());
        Files.writeString(solution, "사용자 풀이");
        assertEquals(1, run("import", URL, "--output", output));
        assertTrue(stderr.toString().contains("FILE_ALREADY_EXISTS"));
        assertEquals("사용자 풀이", Files.readString(solution));
        assertEquals(0, run("import", "--force", "--output", output, URL));
        assertTrue(Files.readString(solution).contains("class Solution"));
    }

    @Test
    void debugIncludesStepsAndHelpDoesNotFetch() {
        assertEquals(0, run("import", URL, "--output", directory.toString(), "--debug"));
        assertTrue(stderr.toString().contains("시그니처 분석"));
        assertTrue(stderr.toString().contains("SolutionTest.java 생성"));
        assertEquals(0, Main.run(new String[]{"--help"}, new PrintWriter(stdout), new PrintWriter(stderr), url -> {
            fail("도움말은 네트워크를 호출하면 안 됩니다."); return null;
        }));
    }

    @Test
    void invalidUrlAndFetchFailureAreReportedWithoutFiles() throws Exception {
        assertEquals(1, run("import", "https://example.com", "--output", directory.toString()));
        assertTrue(stderr.toString().contains("INVALID_URL"));
        assertEquals(1, Main.run(new String[]{"import", URL, "--output", directory.toString()},
                new PrintWriter(stdout), new PrintWriter(stderr), url -> {
                    throw new RunnerException(ErrorCode.FETCH_FAILED, "연결 실패");
                }));
        assertTrue(stderr.toString().contains("FETCH_FAILED"));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }

    @ParameterizedTest
    @ValueSource(strings = {"--unknown", "--output"})
    void badOptionsReturnUsageError(String option) {
        assertEquals(2, run("import", URL, option));
        assertTrue(stderr.toString().contains("명령 오류"));
    }

    @Test
    void missingAndExtraUrlsAreUsageErrors() {
        assertEquals(2, run("import"));
        assertEquals(2, run("import", URL, URL));
        assertEquals(2, run("submit", URL));
    }
}
