package dev.programmers.runner.core.domain;

import java.util.Objects;
import javax.lang.model.SourceVersion;

public record Parameter(String name, Type type) {
    public Parameter {
        if (!SourceVersion.isIdentifier(name) || SourceVersion.isKeyword(name))
            throw new IllegalArgumentException("잘못된 Java 매개변수 이름: " + name);
        Objects.requireNonNull(type);
    }
}
