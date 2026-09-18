package dev.programmers.runner.core.generator;

import java.nio.file.Path;
import java.util.Objects;

public record GeneratedSource(Path relativePath, String content) {
    public GeneratedSource {
        Objects.requireNonNull(relativePath);
        Objects.requireNonNull(content);
        if (relativePath.isAbsolute() || relativePath.normalize().startsWith("..")
                || !relativePath.normalize().equals(relativePath) || relativePath.toString().isBlank())
            throw new IllegalArgumentException("생성 경로는 프로젝트 내부 상대 경로여야 합니다.");
    }
}
