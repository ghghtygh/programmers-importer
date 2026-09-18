package dev.programmers.runner.core.fetch;

import dev.programmers.runner.core.domain.ProblemUrl;

@FunctionalInterface
public interface ProblemFetcher {
    RawProblem fetch(ProblemUrl url);
}
