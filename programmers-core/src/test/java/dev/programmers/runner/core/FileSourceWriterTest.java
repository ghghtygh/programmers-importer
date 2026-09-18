package dev.programmers.runner.core;

import dev.programmers.runner.core.error.*;
import dev.programmers.runner.core.generator.GeneratedSource;
import dev.programmers.runner.core.writer.FileSourceWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FileSourceWriterTest {
    @TempDir Path root;
    private final FileSourceWriter writer = new FileSourceWriter();
    private final List<GeneratedSource> sources = List.of(
            new GeneratedSource(Path.of("src/main/java/programmers/p1/Solution.java"), "풀이"),
            new GeneratedSource(Path.of("src/test/java/programmers/p1/SolutionTest.java"), "테스트"));

    @Test
    void writesUtf8AndRefusesToOverwriteEitherFile() throws Exception {
        var paths = writer.write(root, sources, false);
        Files.writeString(paths.get(0), "사용자 풀이");
        var error = assertThrows(RunnerException.class, () -> writer.write(root, sources, false));
        assertEquals(ErrorCode.FILE_ALREADY_EXISTS, error.code());
        assertEquals("사용자 풀이", Files.readString(paths.get(0)));
        assertEquals("테스트", Files.readString(paths.get(1)));
    }

    @Test
    void secondFileConflictDoesNotCreateFirst() throws Exception {
        Path test = root.resolve(sources.get(1).relativePath());
        Files.createDirectories(test.getParent());
        Files.writeString(test, "기존 테스트");
        assertThrows(RunnerException.class, () -> writer.write(root, sources, false));
        assertFalse(Files.exists(root.resolve(sources.get(0).relativePath())));
        assertEquals("기존 테스트", Files.readString(test));
    }

    @Test
    void forceReplacesBothFiles() throws Exception {
        var paths = writer.write(root, sources, false);
        Files.writeString(paths.get(0), "기존 풀이");
        Files.writeString(paths.get(1), "기존 테스트");
        writer.write(root, sources, true);
        assertEquals("풀이", Files.readString(paths.get(0)));
        assertEquals("테스트", Files.readString(paths.get(1)));
    }

    @Test
    void stagingFailureLeavesExistingSolutionUntouched() throws Exception {
        Path main = root.resolve(sources.get(0).relativePath());
        Files.createDirectories(main.getParent());
        Files.writeString(main, "사용자 풀이");
        Files.writeString(root.resolve("src/test"), "디렉터리 아님");
        assertEquals(ErrorCode.FILE_WRITE_FAILED, assertThrows(RunnerException.class,
                () -> writer.write(root, sources, true)).code());
        assertEquals("사용자 풀이", Files.readString(main));
        try (var files = Files.walk(root)) { assertFalse(files.anyMatch(p -> p.toString().endsWith(".tmp"))); }
    }

    @Test
    void doesNotFollowOutputSymlinksEvenWithForce() throws Exception {
        Path outside = Files.createTempDirectory(root, "outside");
        Files.createSymbolicLink(root.resolve("src"), outside);
        assertEquals(ErrorCode.FILE_WRITE_FAILED, assertThrows(RunnerException.class,
                () -> writer.write(root, sources, true)).code());
        try (var children = Files.list(outside)) { assertEquals(0, children.count()); }
    }

    @Test
    void rejectsPathTraversalAndDuplicates() {
        assertThrows(IllegalArgumentException.class, () -> new GeneratedSource(Path.of("../outside.java"), ""));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedSource(Path.of("/outside.java"), ""));
        assertEquals(ErrorCode.FILE_WRITE_FAILED, assertThrows(RunnerException.class,
                () -> writer.write(root, List.of(sources.get(0), sources.get(0)), true)).code());
    }
}
