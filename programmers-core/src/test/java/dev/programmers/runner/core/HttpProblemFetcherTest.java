package dev.programmers.runner.core;

import dev.programmers.runner.core.domain.ProblemUrl;
import dev.programmers.runner.core.error.*;
import dev.programmers.runner.core.fetch.HttpProblemFetcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpProblemFetcherTest {
    @Test
    void requestsJavaAndReturnsRawHtml() {
        var client = new StubClient(200, "text/html; charset=utf-8", "<html>문제</html>");
        var raw = new HttpProblemFetcher(client).fetch(new ProblemUrl(30, 12909));
        assertEquals("<html>문제</html>", raw.html());
        assertEquals("language=java", client.request.uri().getQuery());
        assertEquals(Duration.ofSeconds(30), client.request.timeout().orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 403, 404, 429, 500})
    void reportsHttpErrors(int status) {
        var fetcher = new HttpProblemFetcher(new StubClient(status, "text/html", "<html>오류</html>"));
        var error = assertThrows(RunnerException.class, () -> fetcher.fetch(new ProblemUrl(30, 1)));
        assertEquals(ErrorCode.FETCH_FAILED, error.code());
        assertTrue(error.getMessage().contains(Integer.toString(status)));
    }

    @Test
    void rejectsNonHtmlAndEmptyResponses() {
        for (StubClient client : List.of(new StubClient(200, "application/json", "{}"), new StubClient(200, "text/html", " "))) {
            assertEquals(ErrorCode.FETCH_FAILED, assertThrows(RunnerException.class,
                    () -> new HttpProblemFetcher(client).fetch(new ProblemUrl(30, 1))).code());
        }
    }

    @Test
    void reportsIoFailureAndPreservesInterrupt() {
        var client = new StubClient(200, "text/html", "html");
        client.ioFailure = true;
        assertEquals(ErrorCode.FETCH_FAILED, assertThrows(RunnerException.class,
                () -> new HttpProblemFetcher(client).fetch(new ProblemUrl(30, 1))).code());
        client.ioFailure = false;
        client.interrupted = true;
        try {
            assertThrows(RunnerException.class, () -> new HttpProblemFetcher(client).fetch(new ProblemUrl(30, 1)));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }

    private static final class StubClient extends HttpClient {
        private final int status;
        private final String contentType;
        private final String body;
        HttpRequest request;
        boolean ioFailure;
        boolean interrupted;

        StubClient(int status, String contentType, String body) {
            this.status = status; this.contentType = contentType; this.body = body;
        }

        @Override @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
            if (ioFailure) throw new IOException("연결 실패");
            if (interrupted) throw new InterruptedException();
            this.request = request;
            return new HttpResponse<>() {
                public int statusCode() { return status; }
                public HttpRequest request() { return request; }
                public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
                public HttpHeaders headers() { return HttpHeaders.of(Map.of("Content-Type", List.of(contentType)), (a, b) -> true); }
                public T body() { return (T) body; }
                public Optional<SSLSession> sslSession() { return Optional.empty(); }
                public URI uri() { return request.uri(); }
                public Version version() { return Version.HTTP_1_1; }
            };
        }

        public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        public Optional<Duration> connectTimeout() { return Optional.of(Duration.ofSeconds(10)); }
        public Redirect followRedirects() { return Redirect.NEVER; }
        public Optional<ProxySelector> proxy() { return Optional.empty(); }
        public SSLContext sslContext() { throw new UnsupportedOperationException(); }
        public SSLParameters sslParameters() { return new SSLParameters(); }
        public Optional<Authenticator> authenticator() { return Optional.empty(); }
        public Version version() { return Version.HTTP_1_1; }
        public Optional<Executor> executor() { return Optional.empty(); }
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h) { throw new UnsupportedOperationException(); }
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h, HttpResponse.PushPromiseHandler<T> p) { throw new UnsupportedOperationException(); }
    }
}
