package dev.programmers.runner.core;

import dev.programmers.runner.core.domain.*;
import dev.programmers.runner.core.error.*;
import dev.programmers.runner.core.fetch.RawProblem;
import dev.programmers.runner.core.parser.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class HtmlProblemParserTest {
    static Problem fixture(int id) throws IOException {
        try (var stream = HtmlProblemParserTest.class.getResourceAsStream("/problems/" + id + ".html")) {
            assertNotNull(stream);
            return new HtmlProblemParser().parse(new RawProblem(new ProblemUrl(30, id), new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        }
    }

    static Problem html(String source, String body) {
        String escaped = source.replace("&", "&amp;").replace("<", "&lt;");
        return new HtmlProblemParser().parse(new RawProblem(new ProblemUrl(30, 1),
                "<title>테스트</title><textarea>" + escaped + "</textarea>" + body));
    }

    @ParameterizedTest
    @CsvSource({"12909,4,boolean,1", "468381,6,int,1", "150370,2,int[],3"})
    void parsesRealPageFragments(int id, int count, String returnType, int parameters) throws IOException {
        Problem problem = fixture(id);
        assertEquals(count, problem.testCases().size());
        assertEquals(Type.parse(returnType), problem.method().returnType());
        assertEquals(parameters, problem.method().parameters().size());
        assertFalse(problem.title().isBlank());
        assertTrue(problem.starterSource().contains("class Solution"));
    }

    @Test
    void matchesColumnsByNameAndPreservesWhitespace() {
        Problem p = html("class Solution { public String solution(int n, String s) {return s;} }", """
                <h5>제한사항</h5><table><tr><th>그룹</th><th>설명</th></tr><tr><td>1</td><td>무관</td></tr></table>
                <section><h4>입출력 예</h4><div><table class="changed">
                <tr><td>result</td><td>s</td><td>n</td></tr>
                <tr><td>"a  &amp;  b"</td><td><code>"a  &amp;  b"</code></td><td>3</td></tr>
                </table></div></section>
                """);
        assertEquals(new Value.NumberValue(new java.math.BigDecimal("3")), p.testCases().get(0).arguments().get(0));
        assertEquals(new Value.StringValue("a  &  b"), p.testCases().get(0).arguments().get(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<table><tr><th>x</th><th>result</th></tr></table>",
            "<table><tr><th>x</th><th>result</th></tr><tr><td>1</td></tr></table>",
            "<table><tr><th>x</th><th>result</th></tr><tr><td>1.5</td><td>1</td></tr></table>",
            "<table><tr><th>other</th><th>result</th></tr><tr><td>1</td><td>1</td></tr></table>",
            "<table><tr><th>x</th><th>result</th></tr><tr><td>1</td><td>1</td></tr></table>" +
            "<table><tr><th>x</th><th>result</th></tr><tr><td>2</td><td>2</td></tr></table>"
    })
    void rejectsMissingMalformedAndAmbiguousExamples(String body) {
        assertEquals(ErrorCode.EXAMPLE_PARSE_FAILED, assertThrows(ProblemParseException.class,
                () -> html("class Solution {int solution(int x){return 0;}}", body)).code());
    }

    @Test
    void parsesJavaAstInsteadOfCommentsAndRegex() {
        var method = new JavaSignatureParser().parse("""
                // public float solution(char fake) { }
                class Solution {
                    @Deprecated
                    public long[] solution(final int grid[][], java.lang.String text) throws Exception {
                        return null;
                    }
                }
                """);
        assertEquals(Type.parse("int[][]"), method.parameters().get(0).type());
        assertEquals(Type.parse("long[]"), method.returnType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"class Solution {void solution(){}}", "class Solution {int solution(int... a){return 0;}}",
            "class Solution {int solution(int[][][] a){return 0;}}", "class Solution {int solution(){return 0;} int solution(int x){return 0;}}",
            "class Main {public static void main(String[] args){}}", "class Solution {private int solution(){return 0;}}", "bad code"})
    void rejectsUnsupportedSignatures(String source) {
        assertThrows(ProblemParseException.class, () -> new JavaSignatureParser().parse(source));
    }

    @Test
    void prefersJavaInitialCodeOverEditedTextarea() {
        String html = """
                <input id="initial_code_1" value="class Solution {int solution(int x){return 0;}}">
                <input id="1" data-language="java" value="edited">
                <textarea>broken edited source</textarea>
                <table><tr><th>x</th><th>result</th></tr><tr><td>1</td><td>1</td></tr></table>
                """;
        var problem = new HtmlProblemParser().parse(new RawProblem(new ProblemUrl(30, 1), html));
        assertEquals(Type.parse("int"), problem.method().returnType());
    }
}
