package dev.programmers.runner.core.parser;

import dev.programmers.runner.core.domain.Problem;
import dev.programmers.runner.core.fetch.RawProblem;

@FunctionalInterface
public interface ProblemParser {
    Problem parse(RawProblem rawProblem);
}
