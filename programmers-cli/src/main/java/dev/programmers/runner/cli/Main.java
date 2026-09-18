package dev.programmers.runner.cli;

import dev.programmers.runner.core.ProblemImporter;
import dev.programmers.runner.core.error.RunnerException;
import dev.programmers.runner.core.fetch.*;
import dev.programmers.runner.core.parser.*;
import dev.programmers.runner.core.writer.FileSourceWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class Main {
    public static void main(String[] args) {
        int status = run(args, new PrintWriter(System.out, true), new PrintWriter(System.err, true), new HttpProblemFetcher());
        if (status != 0) System.exit(status);
    }

    public static int run(String[] args, PrintWriter out, PrintWriter err, ProblemFetcher fetcher) {
        boolean debug = false;
        try {
            if (args.length == 0 || (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h")))) {
                help(out); return 0;
            }
            if (args.length == 1 && args[0].equals("--version")) { out.println("Programmers Local Runner 0.1.0"); return 0; }
            if (!args[0].equals("import")) throw new IllegalArgumentException("지원하지 않는 명령: " + args[0]);
            String url = null;
            Path output = Path.of(".");
            boolean force = false;
            boolean outputSet = false;
            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "--force" -> force = true;
                    case "--debug" -> debug = true;
                    case "--help", "-h" -> { help(out); return 0; }
                    case "--output" -> {
                        if (outputSet || ++i == args.length || args[i].startsWith("--") || args[i].isBlank())
                            throw new IllegalArgumentException("--output 뒤에 출력 경로를 한 번 지정하세요.");
                        output = Path.of(args[i]);
                        outputSet = true;
                    }
                    default -> {
                        if (args[i].startsWith("-")) throw new IllegalArgumentException("알 수 없는 옵션: " + args[i]);
                        if (url != null) throw new IllegalArgumentException("문제 URL은 하나만 지정하세요.");
                        url = args[i];
                    }
                }
            }
            if (url == null) throw new IllegalArgumentException("문제 URL을 지정하세요.");
            Consumer<String> progress = debug ? err::println : ignored -> {};
            var importer = new ProblemImporter(fetcher, new HtmlProblemParser(new JavaSignatureParser(), progress),
                    new FileSourceWriter(), progress);
            var result = importer.importProblem(url, output, force);
            out.println("✓ 문제 " + result.problem().id() + " 불러옴: " + result.problem().title());
            for (Path path : result.files()) out.println("✓ 생성: " + path);
            out.println("공개 예제 " + result.problem().testCases().size() + "개를 가져왔습니다.");
            return 0;
        } catch (RunnerException e) {
            err.println("[" + e.code() + "] " + e.getMessage());
            if (debug) e.printStackTrace(err);
            return 1;
        } catch (IllegalArgumentException e) {
            err.println("명령 오류: " + e.getMessage());
            help(err);
            return 2;
        } finally {
            out.flush(); err.flush();
        }
    }

    private static void help(PrintWriter out) {
        out.println("사용법: programmers import <URL> [--output <경로>] [--force] [--debug]");
        out.println("  --output  파일 생성 기준 경로 (기본값: 현재 디렉터리)");
        out.println("  --force   기존 Solution.java와 SolutionTest.java 덮어쓰기");
        out.println("  --debug   단계별 로그 및 오류 상세 출력");
        out.println("  --help    도움말 출력");
        out.println("  --version 버전 출력");
    }
}
