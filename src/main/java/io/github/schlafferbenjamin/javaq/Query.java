package io.github.schlafferbenjamin.javaq;

import io.github.schlafferbenjamin.javaq.internal.AbstractIterator;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;


@FunctionalInterface
public interface Query<T> extends Iterable<T> {
    default Query<T> where(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return () -> new AbstractIterator<>() {
            private final Iterator<T> source = iterator();

            @Override
            protected T computeNext() {
                while (source.hasNext()) {
                    T item = source.next();
                    if (predicate.test(item)) {
                        return item;
                    }
                }
                return endOfData();
            }
        };
    }

    default <R> Query<R> select(Function<? super T, ? extends R> selector) {
        Objects.requireNonNull(selector, "selector");
        return () -> new AbstractIterator<R>() {
            private final Iterator<T> source = iterator();

            @Override
            protected R computeNext() {
                if (!source.hasNext())  return endOfData();

                return selector.apply(source.next());
            }
        };
    }


    default Query<T> take(int count) {
        if (count < 0) throw new IllegalArgumentException("Count must be non-negative");

        return () -> new AbstractIterator<T>() {
            private final Iterator<T> source = iterator();
            private int remaining = count;

            @Override
            protected T computeNext() {
                if (remaining <= 0 || !source.hasNext()) return endOfData();

                remaining--;
                return source.next();
            }
        };
    }


    default List<T> toList() {
        List<T> result = new ArrayList<T>();

        for (T item : this) {
            result.add(item);
        }

        return result;
    }
}