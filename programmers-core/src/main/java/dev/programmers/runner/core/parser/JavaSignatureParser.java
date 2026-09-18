package dev.programmers.runner.core.parser;

import com.github.javaparser.JavaParser;
import dev.programmers.runner.core.domain.*;
import dev.programmers.runner.core.error.*;

public final class JavaSignatureParser implements SignatureParser {
    @Override
    public MethodSignature parse(String source) {
        var result = new JavaParser().parse(source);
        if (!result.isSuccessful() || result.getResult().isEmpty())
            throw new ProblemParseException(ErrorCode.SIGNATURE_PARSE_FAILED, "Java 기본 코드를 분석하지 못했습니다.");
        var unit = result.getResult().orElseThrow();
        var solution = unit.getClassByName("Solution").orElseThrow(() ->
                new ProblemParseException(ErrorCode.UNSUPPORTED_PROBLEM, "Solution 클래스가 없습니다."));
        if (solution.isInterface() || solution.isAbstract() || !solution.getTypeParameters().isEmpty()
                || !solution.getExtendedTypes().isEmpty() || !solution.getImplementedTypes().isEmpty()
                || !solution.getConstructors().isEmpty()
                || unit.getTypes().stream().anyMatch(t -> t != solution && t.isPublic()))
            throw unsupported();
        var methods = solution.getMethodsByName("solution");
        if (methods.size() != 1)
            throw new ProblemParseException(ErrorCode.SIGNATURE_PARSE_FAILED, "solution 메서드를 하나로 식별할 수 없습니다.");
        var method = methods.get(0);
        if (method.isPrivate() || method.isAbstract() || method.getBody().isEmpty()
                || !method.getTypeParameters().isEmpty() || method.getParameters().stream().anyMatch(p -> p.isVarArgs()))
            throw unsupported();
        try {
            return new MethodSignature("Solution", "solution", Type.parse(method.getType().asString()),
                    method.getParameters().stream()
                            .map(p -> new Parameter(p.getNameAsString(), Type.parse(p.getType().asString()))).toList());
        } catch (IllegalArgumentException e) {
            throw new ProblemParseException(ErrorCode.SIGNATURE_PARSE_FAILED, "잘못된 Java 시그니처입니다.", e);
        }
    }

    private static ProblemParseException unsupported() {
        return new ProblemParseException(ErrorCode.UNSUPPORTED_PROBLEM, "기본 Solution.solution 함수 형태만 지원합니다.");
    }
}
