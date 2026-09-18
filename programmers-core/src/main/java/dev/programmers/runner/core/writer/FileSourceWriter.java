package dev.programmers.runner.core.writer;

import dev.programmers.runner.core.error.*;
import dev.programmers.runner.core.generator.GeneratedSource;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/** 전체 사전 검사와 임시 파일 준비 후 교체합니다. 쓰기 실패 시 완료한 파일을 복구합니다. */
public final class FileSourceWriter implements SourceWriter {
    @Override
    public List<Path> write(Path output, List<GeneratedSource> sources, boolean force) {
        Path root = output.toAbsolutePath().normalize();
        var paths = sources.stream().map(s -> root.resolve(s.relativePath()).normalize()).toList();
        if (paths.stream().anyMatch(p -> !p.startsWith(root)) || new HashSet<>(paths).size() != paths.size())
            throw new RunnerException(ErrorCode.FILE_WRITE_FAILED, "생성 경로가 유효하지 않습니다.");
        var staged = new ArrayList<Path>();
        var backups = new HashMap<Path, byte[]>();
        var committed = new ArrayList<Path>();
        try {
            for (Path path : paths) {
                checkPath(root, path);
                if (Files.exists(path, NOFOLLOW_LINKS)) {
                    if (!force) throw new RunnerException(ErrorCode.FILE_ALREADY_EXISTS, "이미 파일이 존재합니다: " + path);
                    if (!Files.isRegularFile(path, NOFOLLOW_LINKS)) throw new IOException("일반 파일이 아닙니다: " + path);
                    backups.put(path, Files.readAllBytes(path));
                }
            }
            for (int i = 0; i < paths.size(); i++) {
                Path path = paths.get(i);
                Files.createDirectories(path.getParent());
                checkPath(root, path);
                Path temporary = Files.createTempFile(path.getParent(), ".programmers-", ".tmp");
                staged.add(temporary);
                Files.writeString(temporary, sources.get(i).content());
            }
            for (int i = 0; i < paths.size(); i++) {
                Path path = paths.get(i);
                checkPath(root, path);
                if (force) Files.move(staged.get(i), path, StandardCopyOption.REPLACE_EXISTING);
                else Files.move(staged.get(i), path);
                committed.add(path);
            }
            return paths;
        } catch (IOException | RunnerException e) {
            for (int i = committed.size() - 1; i >= 0; i--) {
                Path path = committed.get(i);
                try {
                    if (backups.containsKey(path)) Files.write(path, backups.get(path));
                    else Files.deleteIfExists(path);
                } catch (IOException rollback) { e.addSuppressed(rollback); }
            }
            if (e instanceof RunnerException runner) throw runner;
            ErrorCode code = e instanceof FileAlreadyExistsException ? ErrorCode.FILE_ALREADY_EXISTS : ErrorCode.FILE_WRITE_FAILED;
            throw new RunnerException(code, "파일을 저장하지 못했습니다: " + e.getMessage(), e);
        } finally {
            for (Path path : staged) {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { /* 원래 오류를 보존합니다. */ }
            }
        }
    }

    private static void checkPath(Path root, Path path) throws IOException {
        // 출력 루트 자체는 사용자가 선택한 위치입니다. 내부 링크를 통한 경로 이탈은 거부합니다.
        Path current = root;
        for (Path component : root.relativize(path)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) throw new IOException("출력 경로 내부의 심볼릭 링크는 지원하지 않습니다: " + current);
        }
    }
}
