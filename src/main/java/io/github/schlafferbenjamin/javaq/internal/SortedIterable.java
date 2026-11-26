package io.github.schlafferbenjamin.javaq.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class SortedIterable<T> implements Iterable<T> {
    private final Iterable<T> source;
    private final Comparator<? super T> comparator;

    public SortedIterable(Iterable<T> source, Comparator<? super T> comparator) {
        this.source = Objects.requireNonNull(source, "source");
        this.comparator = Objects.requireNonNull(comparator, "comparator");
    }

    @Override
    public Iterator<T> iterator() {
        List<T> buffer = new ArrayList<>();
        for (T item : source) {
            buffer.add(item);
        }
        buffer.sort(comparator);
        return buffer.iterator();
    }
}