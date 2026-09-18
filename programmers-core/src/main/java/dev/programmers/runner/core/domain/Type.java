package dev.programmers.runner.core.domain;

import dev.programmers.runner.core.error.ErrorCode;
import dev.programmers.runner.core.error.ProblemParseException;
import java.util.Objects;

public sealed interface Type permits Type.PrimitiveType, Type.StringType, Type.ArrayType {
    enum PrimitiveType implements Type { INT, LONG, DOUBLE, BOOLEAN }
    enum StringType implements Type { STRING }
    record ArrayType(Type component) implements Type {
        public ArrayType { Objects.requireNonNull(component); }
    }

    static Type parse(String source) {
        String name = source.replaceAll("\\s+", "");
        int dimensions = 0;
        while (name.endsWith("[]")) {
            dimensions++;
            name = name.substring(0, name.length() - 2);
        }
        if (dimensions > 2) throw unsupported(source);
        Type type = switch (name) {
            case "int" -> PrimitiveType.INT;
            case "long" -> PrimitiveType.LONG;
            case "double" -> PrimitiveType.DOUBLE;
            case "boolean" -> PrimitiveType.BOOLEAN;
            case "String", "java.lang.String" -> StringType.STRING;
            default -> throw unsupported(source);
        };
        for (int i = 0; i < dimensions; i++) type = new ArrayType(type);
        return type;
    }

    private static ProblemParseException unsupported(String source) {
        return new ProblemParseException(ErrorCode.UNSUPPORTED_PROBLEM, "지원하지 않는 Java 타입: " + source);
    }
}
