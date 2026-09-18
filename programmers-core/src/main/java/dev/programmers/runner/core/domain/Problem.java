package dev.programmers.runner.core.domain;

import java.util.List;
import java.util.Objects;

public record Problem(long id, String title, MethodSignature method, List<TestCase> testCases,
                      String starterSource) {
    public Problem(long id, String title, MethodSignature method, List<TestCase> testCases) {
        this(id, title, method, testCases, null);
    }

    public Problem {
        if (id <= 0) throw new IllegalArgumentException("문제 ID는 양수여야 합니다.");
        Objects.requireNonNull(title);
        Objects.requireNonNull(method);
        testCases = List.copyOf(testCases);
        if (testCases.isEmpty()) throw new IllegalArgumentException("공개 예제가 없습니다.");
        for (TestCase test : testCases) {
            if (test.arguments().size() != method.parameters().size())
                throw new IllegalArgumentException("예제 인수의 개수가 시그니처와 다릅니다.");
        }
    }

    public String packageName() { return "programmers.p" + id; }
}
