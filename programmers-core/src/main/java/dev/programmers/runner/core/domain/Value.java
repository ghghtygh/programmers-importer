package dev.programmers.runner.core.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public sealed interface Value permits Value.NumberValue, Value.StringValue,
        Value.BooleanValue, Value.ArrayValue, Value.NullValue {
    record NumberValue(BigDecimal value) implements Value {
        public NumberValue { Objects.requireNonNull(value); }
    }
    record StringValue(String value) implements Value {
        public StringValue { Objects.requireNonNull(value); }
    }
    record BooleanValue(boolean value) implements Value {}
    record ArrayValue(List<Value> values) implements Value {
        public ArrayValue { values = List.copyOf(values); }
    }
    enum NullValue implements Value { NULL }
}
