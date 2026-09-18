package dev.programmers.runner.core.parser;

import dev.programmers.runner.core.domain.MethodSignature;

@FunctionalInterface
public interface SignatureParser {
    MethodSignature parse(String source);
}
