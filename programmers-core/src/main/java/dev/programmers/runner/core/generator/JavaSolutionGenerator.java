package dev.programmers.runner.core.generator;

import com.github.javaparser.JavaParser;
import dev.programmers.runner.core.domain.Problem;
import dev.programmers.runner.core.error.*;
import dev.programmers.runner.core.parser.JavaSignatureParser;
import java.nio.file.Path;
import java.util.stream.Collectors;

public final class JavaSolutionGenerator implements SolutionGenerator {
    private final JavaTypeRenderer types = new JavaTypeRenderer();

    @Override
    public GeneratedSource generate(Problem problem) {
        String source;
        if (problem.starterSource() != null && !problem.starterSource().isBlank()) {
            if (!new JavaSignatureParser().parse(problem.starterSource()).equals(problem.method()))
                throw new ProblemParseException(ErrorCode.SIGNATURE_PARSE_FAILED, "기본 코드와 문제 시그니처가 다릅니다.");
            var unit = new JavaParser().parse(problem.starterSource()).getResult().orElseThrow();
            unit.setPackageDeclaration(problem.packageName());
            unit.getClassByName("Solution").orElseThrow().setPublic(true);
            source = unit.toString();
        } else {
            String parameters = problem.method().parameters().stream()
                    .map(p -> types.render(p.type()) + " " + p.name()).collect(Collectors.joining(", "));
            source = "package " + problem.packageName() + ";\n\npublic class Solution {\n\n"
                    + "    public " + types.render(problem.method().returnType()) + " solution(" + parameters + ") {\n"
                    + "        // TODO: 풀이를 구현하세요.\n"
                    + "        return " + types.defaultValue(problem.method().returnType()) + ";\n    }\n}\n";
        }
        return new GeneratedSource(Path.of("src/main/java/programmers/p" + problem.id() + "/Solution.java"), source);
    }
}
