package dev.programmers.runner.core.parser;

import dev.programmers.runner.core.domain.Value;
import dev.programmers.runner.core.domain.Value.*;
import dev.programmers.runner.core.error.ErrorCode;
import dev.programmers.runner.core.error.ProblemParseException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.regex.Pattern;

/** JSON 형태의 공개 예제를 실행 없이 값 트리로 변환합니다. */
public final class ValueParser {
    private static final Pattern NUMBER = Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");

    public Value parse(String text) {
        if (text == null || text.length() > 1_000_000) throw error(0, "값이 없거나 너무 큽니다.");
        var cursor = new Cursor(text);
        Value value = cursor.value(0);
        cursor.space();
        if (cursor.position != text.length()) throw error(cursor.position, "값 뒤에 불필요한 내용이 있습니다.");
        return value;
    }

    private static ProblemParseException error(int position, String reason) {
        return new ProblemParseException(ErrorCode.EXAMPLE_PARSE_FAILED, "예제 값 위치 " + position + ": " + reason);
    }

    private static final class Cursor {
        private final String text;
        private int position;
        Cursor(String text) { this.text = text; }

        void space() {
            while (position < text.length() && (Character.isWhitespace(text.charAt(position))
                    || text.charAt(position) == '\u00a0')) position++;
        }

        Value value(int depth) {
            space();
            if (depth > 32 || position == text.length()) throw error(position, "값이 없거나 중첩이 너무 깊습니다.");
            char c = text.charAt(position);
            if (c == '"') return new StringValue(string());
            if (c == '[') {
                position++;
                var values = new ArrayList<Value>();
                space();
                if (take(']')) return new ArrayValue(values);
                do { values.add(value(depth + 1)); space(); } while (take(','));
                if (!take(']')) throw error(position, "배열의 닫는 괄호가 필요합니다.");
                return new ArrayValue(values);
            }
            if (literal("true")) return new BooleanValue(true);
            if (literal("false")) return new BooleanValue(false);
            if (literal("null")) return NullValue.NULL;
            var matcher = NUMBER.matcher(text).region(position, text.length());
            if (!matcher.lookingAt()) throw error(position, "숫자, 문자열, 불리언 또는 배열이 필요합니다.");
            String number = matcher.group();
            position = matcher.end();
            try {
                if (number.length() > 1000) throw new NumberFormatException();
                BigDecimal parsed = new BigDecimal(number);
                if (Math.abs((long) parsed.scale()) > 10000) throw new NumberFormatException();
                return new NumberValue(parsed);
            } catch (NumberFormatException e) {
                throw error(position, "숫자 범위가 너무 큽니다.");
            }
        }

        boolean literal(String value) {
            if (!text.startsWith(value, position)) return false;
            position += value.length();
            return true;
        }

        boolean take(char c) {
            if (position < text.length() && text.charAt(position) == c) { position++; return true; }
            return false;
        }

        String string() {
            position++;
            var result = new StringBuilder();
            while (position < text.length()) {
                char c = text.charAt(position++);
                if (c == '"') return result.toString();
                if (c < 32) throw error(position, "문자열 제어 문자는 이스케이프해야 합니다.");
                if (c != '\\') { result.append(c); continue; }
                if (position == text.length()) throw error(position, "문자열 이스케이프가 불완전합니다.");
                char escaped = text.charAt(position++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (position + 4 > text.length()) throw error(position, "유니코드 이스케이프가 불완전합니다.");
                        String hex = text.substring(position, position + 4);
                        if (!hex.matches("[0-9a-fA-F]{4}")) throw error(position, "잘못된 유니코드 이스케이프입니다.");
                        result.append((char) Integer.parseInt(hex, 16));
                        position += 4;
                    }
                    default -> throw error(position, "지원하지 않는 문자열 이스케이프입니다.");
                }
            }
            throw error(position, "문자열의 닫는 따옴표가 필요합니다.");
        }
    }
}
