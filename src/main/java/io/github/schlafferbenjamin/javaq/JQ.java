package io.github.schlafferbenjamin.javaq;

import java.util.List;
import java.util.Objects;

public final class JQ {
    private JQ() {};


    public static <T> Query<T> from(Iterable<T> source) {
        Objects.requireNonNull(source, "source");
        return source::iterator;
    }

    @SafeVarargs
    public static <T> Query<T> of(T... items) {
        Objects.requireNonNull(items, "items");
        return () -> List.of(items).iterator();
    }
}