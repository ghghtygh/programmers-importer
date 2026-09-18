package dev.programmers.runner.core.domain;

import dev.programmers.runner.core.error.ErrorCode;
import dev.programmers.runner.core.error.RunnerException;
import java.net.URI;
import java.util.regex.Pattern;

public record ProblemUrl(long courseId, long problemId) {
    private static final Pattern PATH = Pattern.compile("/learn/courses/([0-9]+)/lessons/([0-9]+)/?");

    public ProblemUrl {
        if (courseId <= 0 || problemId <= 0)
            throw invalid();
    }

    public static ProblemUrl parse(String input) {
        try {
            URI uri = URI.create(input.strip());
            var match = PATH.matcher(uri.getRawPath() == null ? "" : uri.getRawPath());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"school.programmers.co.kr".equalsIgnoreCase(uri.getHost())
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !match.matches()) throw invalid();
            return new ProblemUrl(Long.parseLong(match.group(1)), Long.parseLong(match.group(2)));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw invalid();
        }
    }

    public URI javaUri() {
        return URI.create("https://school.programmers.co.kr/learn/courses/" + courseId
                + "/lessons/" + problemId + "?language=java");
    }

    private static RunnerException invalid() {
        return new RunnerException(ErrorCode.INVALID_URL, "지원하지 않는 프로그래머스 URL입니다.");
    }
}
