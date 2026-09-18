package dev.programmers.runner.core.domain;

import java.util.List;
import java.util.Objects;

public record TestCase(List<Value> arguments, Value expected) {
    public TestCase {
        arguments = List.copyOf(arguments);
        Objects.requireNonNull(expected);
    }
}
