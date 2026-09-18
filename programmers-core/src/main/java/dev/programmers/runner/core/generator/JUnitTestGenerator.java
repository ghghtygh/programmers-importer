package dev.programmers.runner.core.generator;

import dev.programmers.runner.core.domain.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public final class JUnitTestGenerator implements SolutionGenerator {
    private final JavaTypeRenderer types = new JavaTypeRenderer();
    private final JavaValueRenderer values = new JavaValueRenderer();

    @Override
    public GeneratedSource generate(Problem problem) {
        MethodSignature method = problem.method();
        var names = method.parameters().stream().map(Parameter::name).collect(Collectors.toCollection(HashSet::new));
        String expectedName = "expected";
        while (names.contains(expectedName)) expectedName = "_" + expectedName;
        String parameters = method.parameters().stream().map(p -> types.render(p.type()) + " " + p.name())
                .collect(Collectors.joining(", "));
        if (!parameters.isEmpty()) parameters += ", ";
        parameters += types.render(method.returnType()) + " " + expectedName;
        String arguments = method.parameters().stream().map(Parameter::name).collect(Collectors.joining(", "));
        String assertion = method.returnType() instanceof Type.ArrayType ? "assertArrayEquals" : "assertEquals";
        var examples = new ArrayList<String>();
        for (TestCase test : problem.testCases()) {
            var literals = new ArrayList<String>();
            for (int i = 0; i < method.parameters().size(); i++)
                literals.add(values.render(method.parameters().get(i).type(), test.arguments().get(i)));
            literals.add(values.render(method.returnType(), test.expected()));
            // Object[]를 명시해 단일 참조 배열/2차원 배열의 varargs 펼침을 방지합니다.
            examples.add("            Arguments.of(new Object[]{" + String.join(", ", literals) + "})");
        }
        String source = "package " + problem.packageName() + ";\n\n"
                + "import org.junit.jupiter.params.ParameterizedTest;\n"
                + "import org.junit.jupiter.params.provider.Arguments;\n"
                + "import org.junit.jupiter.params.provider.MethodSource;\n"
                + "import java.util.stream.Stream;\n\n"
                + "import static org.junit.jupiter.api.Assertions." + assertion + ";\n\n"
                + "class SolutionTest {\n\n"
                + "    @ParameterizedTest(name = \"공개 예제 {index}\")\n"
                + "    @MethodSource(\"examples\")\n"
                + "    void matchesExample(" + parameters + ") throws Exception {\n"
                + "        " + assertion + "(" + expectedName + ", new Solution().solution(" + arguments + "));\n"
                + "    }\n\n"
                + "    static Stream<Arguments> examples() {\n"
                + "        return Stream.of(\n" + String.join(",\n", examples) + "\n        );\n"
                + "    }\n}\n";
        return new GeneratedSource(Path.of("src/test/java/programmers/p" + problem.id() + "/SolutionTest.java"), source);
    }
}
