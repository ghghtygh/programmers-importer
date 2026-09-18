package dev.programmers.runner.core.error;

public final class ProblemParseException extends RunnerException {
    public ProblemParseException(ErrorCode code, String detail) { super(code, detail); }
    public ProblemParseException(ErrorCode code, String detail, Throwable cause) { super(code, detail, cause); }
}
