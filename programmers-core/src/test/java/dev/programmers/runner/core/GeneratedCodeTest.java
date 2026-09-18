package dev.programmers.runner.core;

import dev.programmers.runner.core.domain.*;
import dev.programmers.runner.core.generator.*;
import dev.programmers.runner.core.parser.ValueParser;
import dev.programmers.runner.core.writer.FileSourceWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class GeneratedCodeTest {
    @TempDir Path directory;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "int|-2147483648", "long|9223372036854775807", "double|0.125", "boolean|true",
            "String|\"한글  문자열\"", "int[]|[1,-2]", "long[]|[9223372036854775807]",
            "double[]|[1.5,2.25]", "boolean[]|[true,false]", "String[]|[\"a\",null,\"b\"]",
            "int[][]|[[1,2],[],null]", "long[][]|[[9223372036854775807],[]]",
            "double[][]|[[1.5],[2.5,3.5]]", "boolean[][]|[[true],[false]]", "String[][]|[[\"a\"],[null,\"b\"]]"
    })
    void compilesAndExecutesEverySupportedType(String name, String input) throws Exception {
        Type type = Type.parse(name);
        Value value = new ValueParser().parse(input);
        var problem = new Problem(1, "타입 검증", new MethodSignature("Solution", "solution", type,
                List.of(new Parameter("expected", type))), List.of(new TestCase(List.of(value), value)),
                "class Solution { public " + name + " solution(" + name + " expected) {return expected;} }");
        assertEquals(1, execute(problem, null).getTestsSucceededCount());
    }

    @Test
    void escapedStringsRoundTripThroughJavaCompiler() throws Exception {
        String text = "따옴표\" 역슬래시\\ 경로\\u000a\n\t\0\177 😃";
        Value value = new Value.StringValue(text);
        var p = new Problem(1, "이스케이프", new MethodSignature("Solution", "solution", Type.parse("String"),
                List.of(new Parameter("text", Type.parse("String")))), List.of(new TestCase(List.of(value), value)),
                "class Solution {String solution(String text) {return text;}}");
        assertEquals(1, execute(p, null).getTestsSucceededCount());
    }

    @Test
    void singleArrayExpectedIsNotExpandedIntoVarargs() throws Exception {
        Value value = new ValueParser().parse("[\"one\",\"two\"]");
        var p = new Problem(1, "인수 없는 함수", new MethodSignature("Solution", "solution", Type.parse("String[]"), List.of()),
                List.of(new TestCase(List.of(), value)), "class Solution {String[] solution(){return new String[]{\"one\",\"two\"};}}");
        assertEquals(1, execute(p, null).getTestsSucceededCount());
    }

    @Test
    void scalarNullIsOneArgument() throws Exception {
        var p = new Problem(1, "null", new MethodSignature("Solution", "solution", Type.parse("String"), List.of()),
                List.of(new TestCase(List.of(), Value.NullValue.NULL)));
        assertEquals(1, execute(p, null).getTestsSucceededCount());
    }

    @Test
    void nestedArrayMismatchActuallyFails() throws Exception {
        var expected = new ValueParser().parse("[[1,2],[3,4]]");
        var p = new Problem(1, "깊은 비교", new MethodSignature("Solution", "solution", Type.parse("int[][]"), List.of()),
                List.of(new TestCase(List.of(), expected)),
                "class Solution {int[][] solution(){return new int[][]{{1,2},{3,99}};}}");
        var summary = execute(p, null);
        assertEquals(1, summary.getTestsFailedCount());
        assertTrue(summary.getFailures().get(0).getException() instanceof AssertionError);
    }

    @Test
    void realParenthesesExamplesPassWithOnlySolutionImplementation() throws Exception {
        var problem = HtmlProblemParserTest.fixture(12909);
        var summary = execute(problem, """
                package programmers.p12909;
                public class Solution {
                    boolean solution(String s) {
                        int balance = 0;
                        for (char c : s.toCharArray()) {
                            balance += c == '(' ? 1 : -1;
                            if (balance < 0) return false;
                        }
                        return balance == 0;
                    }
                }
                """);
        assertEquals(4, summary.getTestsSucceededCount());
        assertEquals(0, summary.getTestsFailedCount());
    }

    @Test
    void realMultipleParameterExamplesPassWithOnlySolutionImplementation() throws Exception {
        var problem = HtmlProblemParserTest.fixture(150370);
        var summary = execute(problem, """
                package programmers.p150370;
                import java.util.*;
                public class Solution {
                    public int[] solution(String today, String[] terms, String[] privacies) {
                        Map<String, Integer> durations = new HashMap<>();
                        for (String term : terms) {
                            String[] pieces = term.split(" ");
                            durations.put(pieces[0], Integer.parseInt(pieces[1]) * 28);
                        }
                        List<Integer> expired = new ArrayList<>();
                        for (int i = 0; i < privacies.length; i++) {
                            String[] pieces = privacies[i].split(" ");
                            if (day(pieces[0]) + durations.get(pieces[1]) <= day(today)) expired.add(i + 1);
                        }
                        return expired.stream().mapToInt(Integer::intValue).toArray();
                    }
                    private int day(String date) {
                        String[] p = date.split("[.]");
                        return Integer.parseInt(p[0]) * 336 + Integer.parseInt(p[1]) * 28 + Integer.parseInt(p[2]);
                    }
                }
                """);
        assertEquals(2, summary.getTestsSucceededCount());
        assertEquals(0, summary.getTestsFailedCount());
    }

    @Test
    void sixRailExamplesCompileAndRunBeforeUserImplementation() throws Exception {
        var summary = execute(HtmlProblemParserTest.fixture(468381), null);
        assertEquals(6, summary.getTestsStartedCount());
        // 원본 기본 구현은 0을 반환하므로 결과가 0인 마지막 예제만 통과합니다.
        assertEquals(1, summary.getTestsSucceededCount());
        assertEquals(5, summary.getTestsFailedCount());
        assertTrue(summary.getFailures().stream().allMatch(f -> f.getException() instanceof AssertionError));
    }

    @Test
    void generatedTestMatchesSnapshot() throws Exception {
        var value = new ValueParser().parse("[[1,2],[3,4]]");
        var p = new Problem(42, "스냅샷", new MethodSignature("Solution", "solution", Type.parse("int[][]"),
                List.of(new Parameter("grid", Type.parse("int[][]")))), List.of(new TestCase(List.of(value), value)));
        try (var input = getClass().getResourceAsStream("/snapshots/SolutionTest.java.txt")) {
            assertNotNull(input);
            assertEquals(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), new JUnitTestGenerator().generate(p).content());
        }
    }

    @ParameterizedTest
    @CsvSource({"int,0", "long,0L", "double,0.0", "boolean,false", "String,null", "int[],null", "String[][],null"})
    void fallbackStubHasCompilableDefault(String type, String expected) throws Exception {
        Value value = new ValueParser().parse(expected.replace("L", ""));
        var p = new Problem(1, "기본값", new MethodSignature("Solution", "solution", Type.parse(type), List.of()),
                List.of(new TestCase(List.of(), value)));
        assertEquals(1, execute(p, null).getTestsSucceededCount());
    }

    private org.junit.platform.launcher.listeners.TestExecutionSummary execute(Problem problem, String implementation) throws Exception {
        var files = new FileSourceWriter().write(directory,
                List.of(new JavaSolutionGenerator().generate(problem), new JUnitTestGenerator().generate(problem)), false);
        if (implementation != null) Files.writeString(files.get(0), implementation);
        Path classes = Files.createDirectory(directory.resolve("classes"));
        var diagnostics = new java.io.ByteArrayOutputStream();
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, diagnostics,
                "--release", "17", "-encoding", "UTF-8", "-classpath", System.getProperty("generatedTestClasspath"),
                "-d", classes.toString(), files.get(0).toString(), files.get(1).toString());
        assertEquals(0, result, diagnostics.toString(java.nio.charset.StandardCharsets.UTF_8));
        try (var loader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> testClass = Class.forName(problem.packageName() + ".SolutionTest", true, loader);
            var request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(testClass)).build();
            var listener = new SummaryGeneratingListener();
            LauncherFactory.create().execute(request, listener);
            assertEquals(problem.testCases().size(), listener.getSummary().getTestsFoundCount());
            return listener.getSummary();
        }
    }
}
