package dev.programmers.runner.core.generator;

import dev.programmers.runner.core.domain.Type;
import dev.programmers.runner.core.domain.Value;
import dev.programmers.runner.core.domain.Value.*;
import dev.programmers.runner.core.error.*;
import java.util.stream.Collectors;

public final class JavaValueRenderer {
    private final JavaTypeRenderer types = new JavaTypeRenderer();

    public String render(Type type, Value value) {
        if (value == NullValue.NULL) {
            if (type instanceof Type.PrimitiveType) throw mismatch(type);
            return "null";
        }
        if (type instanceof Type.ArrayType array) {
            if (!(value instanceof ArrayValue values)) throw mismatch(type);
            return "new " + types.render(type) + "{" + values.values().stream()
                    .map(v -> render(array.component(), v)).collect(Collectors.joining(", ")) + "}";
        }
        if (type == Type.StringType.STRING && value instanceof StringValue string) return quote(string.value());
        if (type == Type.PrimitiveType.BOOLEAN && value instanceof BooleanValue bool) return Boolean.toString(bool.value());
        if (value instanceof NumberValue number) {
            try {
                if (type == Type.PrimitiveType.INT) return Integer.toString(number.value().intValueExact());
                if (type == Type.PrimitiveType.LONG) return number.value().longValueExact() + "L";
                if (type == Type.PrimitiveType.DOUBLE) {
                    double d = number.value().doubleValue();
                    if (!Double.isFinite(d) || (d == 0 && number.value().signum() != 0)) throw mismatch(type);
                    return Double.toString(d);
                }
            } catch (ArithmeticException e) { throw mismatch(type); }
        }
        throw mismatch(type);
    }

    private ProblemParseException mismatch(Type type) {
        return new ProblemParseException(ErrorCode.EXAMPLE_PARSE_FAILED, "예제 값이 " + types.render(type) + " 타입 또는 범위와 맞지 않습니다.");
    }

    public static String quote(String text) {
        var out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 32 || c == 127) out.append(String.format("\\%03o", (int) c));
                    else if (Character.isSurrogate(c)) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }
}
