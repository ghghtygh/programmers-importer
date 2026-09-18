package dev.programmers.runner.core.fetch;

import dev.programmers.runner.core.domain.ProblemUrl;
import dev.programmers.runner.core.error.ErrorCode;
import dev.programmers.runner.core.error.RunnerException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpProblemFetcher implements ProblemFetcher {
    private final HttpClient client;

    public HttpProblemFetcher() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    public HttpProblemFetcher(HttpClient client) { this.client = client; }

    @Override
    public RawProblem fetch(ProblemUrl url) {
        var request = HttpRequest.newBuilder(url.javaUri()).timeout(Duration.ofSeconds(30))
                .header("Accept", "text/html")
                .header("User-Agent", "ProgrammersLocalRunner/0.1")
                .GET().build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() != 200)
                throw new RunnerException(ErrorCode.FETCH_FAILED, "문제 페이지 응답: HTTP " + response.statusCode());
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.toLowerCase(java.util.Locale.ROOT).contains("text/html") || response.body().isBlank())
                throw new RunnerException(ErrorCode.FETCH_FAILED, "문제 페이지에서 HTML을 받지 못했습니다.");
            return new RawProblem(url, response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RunnerException(ErrorCode.FETCH_FAILED, "문제 조회가 중단되었습니다.", e);
        } catch (IOException e) {
            throw new RunnerException(ErrorCode.FETCH_FAILED, "문제 페이지를 가져오지 못했습니다.", e);
        }
    }
}
