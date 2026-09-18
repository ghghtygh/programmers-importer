package dev.programmers.runner.core.error;

public class RunnerException extends RuntimeException {
    private final ErrorCode code;

    public RunnerException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    public RunnerException(ErrorCode code, String detail, Throwable cause) {
        super(detail, cause);
        this.code = code;
    }

    public ErrorCode code() { return code; }
}
