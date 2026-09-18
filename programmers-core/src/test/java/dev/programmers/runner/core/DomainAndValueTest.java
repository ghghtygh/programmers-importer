package dev.programmers.runner.core;

import dev.programmers.runner.core.domain.*;
import dev.programmers.runner.core.domain.Value.*;
import dev.programmers.runner.core.error.*;
import dev.programmers.runner.core.generator.*;
import dev.programmers.runner.core.parser.ValueParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class DomainAndValueTest {
    private final ValueParser parser = new ValueParser();
    private final JavaValueRenderer renderer = new JavaValueRenderer();

    @ParameterizedTest
    @ValueSource(strings = {
            "https://school.programmers.co.kr/learn/courses/30/lessons/12909",
            "https://school.programmers.co.kr/learn/courses/30/lessons/12909/?language=python3#example",
            " https://SCHOOL.PROGRAMMERS.CO.KR:443/learn/courses/30/lessons/12909 "
    })
    void parsesCanonicalUrl(String input) {
        var url = ProblemUrl.parse(input);
        assertEquals(12909, url.problemId());
        assertEquals("https://school.programmers.co.kr/learn/courses/30/lessons/12909?language=java", url.javaUri().toString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"https://example.com/learn/courses/30/lessons/12909",
            "http://school.programmers.co.kr/learn/courses/30/lessons/12909",
            "https://school.programmers.co.kr/learn/courses/30/lessons/",
            "https://school.programmers.co.kr.evil.test/learn/courses/30/lessons/12909",
            "https://user@school.programmers.co.kr/learn/courses/30/lessons/12909",
            "https://school.programmers.co.kr:444/learn/courses/30/lessons/12909",
            "https://school.programmers.co.kr/learn/courses/30/lessons/0",
            "https://school.programmers.co.kr/learn/courses/30/lessons/99999999999999999999999",
            "https://school.programmers.co.kr/learn/courses/30/lessons/%31",
            "javascript:alert(1)", "not a URL"})
    void rejectsInvalidUrl(String input) {
        assertEquals(ErrorCode.INVALID_URL, assertThrows(RunnerException.class, () -> ProblemUrl.parse(input)).code());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "int|1|1", "int|-1|-1", "long|2147483648|2147483648L",
            "long|-9223372036854775808|-9223372036854775808L", "double|1.5|1.5",
            "double|1e2|100.0", "boolean|true|true", "String|\"hello\"|\"hello\"",
            "int[]|[1,2,3]|new int[]{1, 2, 3}",
            "String[]|[\"a\",\"b\"]|new String[]{\"a\", \"b\"}",
            "int[][]|[[1,2],[3,4]]|new int[][]{new int[]{1, 2}, new int[]{3, 4}}",
            "int[][]|[[],null]|new int[][]{new int[]{}, null}", "String|null|null"
    })
    void rendersTypedValues(String type, String input, String expected) {
        assertEquals(expected, renderer.render(Type.parse(type), parser.parse(input)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "[1,]", "[,1]", "[1 2]", "[1", "01", "+1", "NaN", "Infinity",
            "1; System.exit(0)", "truefalse", "\"unterminated", "\"\\x\"", "\"\\uZZZZ\"", "\"a\nb\"", "{}"})
    void rejectsMalformedValues(String input) {
        assertEquals(ErrorCode.EXAMPLE_PARSE_FAILED,
                assertThrows(ProblemParseException.class, () -> parser.parse(input)).code());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"int|2147483648", "int|1.5", "int|null", "int|true",
            "long|9223372036854775808", "boolean|1", "String|12", "int[]|[[1]]", "int[][]|[1]",
            "double|1e999", "double|1e-999", "int[]|[null]"})
    void rejectsTypeOrRangeMismatch(String type, String input) {
        assertThrows(ProblemParseException.class, () -> renderer.render(Type.parse(type), parser.parse(input)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"int", "long", "double", "boolean", "String", "int[]", "long[]", "double[]",
            "boolean[]", "String[]", "int[][]", "long[][]", "double[][]", "boolean[][]", "String[][]"})
    void rendersEverySupportedType(String type) {
        assertEquals(type, new JavaTypeRenderer().render(Type.parse(type)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"void", "Integer", "List<Integer>", "char", "float", "int[][][]"})
    void rejectsUnsupportedTypes(String type) {
        assertEquals(ErrorCode.UNSUPPORTED_PROBLEM, assertThrows(ProblemParseException.class, () -> Type.parse(type)).code());
    }

    @Test
    void preservesEscapedStringsAndSpacing() {
        assertEquals(new StringValue("a  b\n\"\\한글"), parser.parse("\"a  b\\n\\\"\\\\\\uD55C글\""));
        assertEquals("\"a\\n\\\"\\\\\"", renderer.render(Type.parse("String"), new StringValue("a\n\"\\")));
    }

    @Test
    void rejectsExcessiveNestingAndExponents() {
        assertThrows(ProblemParseException.class, () -> parser.parse("[".repeat(40) + "0" + "]".repeat(40)));
        assertThrows(ProblemParseException.class, () -> parser.parse("1e999999999"));
    }
}
