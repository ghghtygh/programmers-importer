package dev.programmers.runner.core.writer;

import dev.programmers.runner.core.generator.GeneratedSource;
import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface SourceWriter {
    List<Path> write(Path output, List<GeneratedSource> sources, boolean force);
}
