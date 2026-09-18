package dev.programmers.runner.core.generator;

import dev.programmers.runner.core.domain.Type;
import java.util.Locale;

public final class JavaTypeRenderer {
    public String render(Type type) {
        if (type instanceof Type.ArrayType array) return render(array.component()) + "[]";
        if (type instanceof Type.PrimitiveType primitive) return primitive.name().toLowerCase(Locale.ROOT);
        return "String";
    }

    public String defaultValue(Type type) {
        if (type instanceof Type.PrimitiveType primitive) return switch (primitive) {
            case INT -> "0";
            case LONG -> "0L";
            case DOUBLE -> "0.0";
            case BOOLEAN -> "false";
        };
        return "null";
    }
}
