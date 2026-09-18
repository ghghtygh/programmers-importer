package dev.programmers.runner.core.generator;

import dev.programmers.runner.core.domain.Problem;

@FunctionalInterface
public interface SolutionGenerator {
    GeneratedSource generate(Problem problem);
}
