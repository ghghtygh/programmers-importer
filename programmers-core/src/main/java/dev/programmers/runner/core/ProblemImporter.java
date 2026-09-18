package dev.programmers.runner.core;

import dev.programmers.runner.core.domain.ProblemUrl;
import dev.programmers.runner.core.fetch.ProblemFetcher;
import dev.programmers.runner.core.parser.ProblemParser;
import dev.programmers.runner.core.generator.*;
import dev.programmers.runner.core.writer.SourceWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public final class ProblemImporter {
    private final ProblemFetcher fetcher;
    private final ProblemParser parser;
    private final SourceWriter writer;
    private final Consumer<String> progress;

    public ProblemImporter(ProblemFetcher fetcher, ProblemParser parser, SourceWriter writer, Consumer<String> progress) {
        this.fetcher = fetcher;
        this.parser = parser;
        this.writer = writer;
        this.progress = progress;
    }

    public ImportResult importProblem(String input, Path output, boolean force) {
        ProblemUrl url = ProblemUrl.parse(input);
        progress.accept("문제 페이지 조회 중...");
        var problem = parser.parse(fetcher.fetch(url));
        progress.accept("Solution.java 생성 중...");
        var solution = new JavaSolutionGenerator().generate(problem);
        progress.accept("SolutionTest.java 생성 중...");
        var test = new JUnitTestGenerator().generate(problem);
        return new ImportResult(problem, writer.write(output, List.of(solution, test), force));
    }
}
