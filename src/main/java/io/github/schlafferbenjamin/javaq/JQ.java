/**
 * My first dependency
 * @Author Schlaffer-Benjamin
 * @Description This is JQ. A LinQ made for Java.
 * I am aware that there are official methods doing this exact things,
 * like stream api but who cares?
 * If a C# programmer tries Java let them have this. It's gonna make it easier for
 * them to learn the syntax differences and then worry about this
 */

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