package dev.programmers.runner.core;

import dev.programmers.runner.core.domain.Problem;
import java.nio.file.Path;
import java.util.List;

public record ImportResult(Problem problem, List<Path> files) {
    public ImportResult { files = List.copyOf(files); }
}
