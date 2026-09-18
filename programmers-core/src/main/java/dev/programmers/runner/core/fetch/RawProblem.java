package dev.programmers.runner.core.fetch;

import dev.programmers.runner.core.domain.ProblemUrl;
import java.util.Objects;

public record RawProblem(ProblemUrl url, String html) {
    public RawProblem { Objects.requireNonNull(url); Objects.requireNonNull(html); }
}
