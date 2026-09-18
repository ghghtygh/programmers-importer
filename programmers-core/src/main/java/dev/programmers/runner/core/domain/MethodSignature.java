package dev.programmers.runner.core.domain;

import java.util.List;
import java.util.Objects;

public record MethodSignature(String className, String methodName, Type returnType, List<Parameter> parameters) {
    public MethodSignature {
        if (!"Solution".equals(className) || !"solution".equals(methodName))
            throw new IllegalArgumentException("Solution.solution 메서드만 지원합니다.");
        Objects.requireNonNull(returnType);
        parameters = List.copyOf(parameters);
        if (parameters.stream().map(Parameter::name).distinct().count() != parameters.size())
            throw new IllegalArgumentException("매개변수 이름이 중복됩니다.");
    }
}
