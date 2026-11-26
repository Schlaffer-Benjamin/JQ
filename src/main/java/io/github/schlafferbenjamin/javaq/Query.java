package io.github.schlafferbenjamin.javaq;

import io.github.schlafferbenjamin.javaq.internal.AbstractIterator;
import io.github.schlafferbenjamin.javaq.internal.SortedIterable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A lazy, re-iterable sequence of elements supporting LINQ-style query composition.
 *
 * <p>Unlike {@link java.util.stream.Stream}, a {@code Query} is an {@link Iterable} whose
 * intermediate operators defer execution until iteration or a terminal operation occurs, and can
 * generally be iterated more than once.</p>
 *
 * @param <T> the type of elements yielded by this query
 */
@FunctionalInterface
public interface Query<T> extends Iterable<T> {

    /**
     * Filters elements of this query based on a predicate.
     *
     * <p>Evaluation is lazy: elements are tested only as the resulting query is iterated.</p>
     *
     * @param predicate the condition each element must satisfy to be included
     * @return a new lazy {@code Query} containing only elements matching {@code predicate}
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
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

    /**
     * Projects each element of this query into a new form using {@code selector}.
     *
     * <p>Evaluation is lazy: {@code selector} is applied to each element as it is consumed.</p>
     *
     * @param selector the transformation function applied to each element
     * @param <R>      the type of elements produced by {@code selector}
     * @return a new lazy {@code Query} of transformed elements
     * @throws NullPointerException if {@code selector} is {@code null}
     */
    default <R> Query<R> select(Function<? super T, ? extends R> selector) {
        Objects.requireNonNull(selector, "selector");
        return () -> new AbstractIterator<R>() {
            private final Iterator<T> source = iterator();

            @Override
            protected R computeNext() {
                if (!source.hasNext()) {
                    return endOfData();
                }
                return selector.apply(source.next());
            }
        };
    }

    /**
     * Limits this query to at most the first {@code count} elements.
     *
     * <p>Evaluation is lazy and short-circuiting: iteration stops as soon as {@code count}
     * elements have been emitted or this query is exhausted.</p>
     *
     * @param count the maximum number of elements to yield; must be non-negative
     * @return a new lazy {@code Query} yielding at most {@code count} elements
     * @throws IllegalArgumentException if {@code count} is negative
     */
    default Query<T> take(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be non-negative");
        }
        return () -> new AbstractIterator<T>() {
            private final Iterator<T> source = iterator();
            private int remaining = count;

            @Override
            protected T computeNext() {
                if (remaining <= 0 || !source.hasNext()) {
                    return endOfData();
                }
                remaining--;
                return source.next();
            }
        };
    }

    /**
     * Materializes all elements of this query into a new mutable {@link List}.
     *
     * @return a {@link List} containing all elements produced by iterating this query
     */
    default List<T> toList() {
        List<T> result = new ArrayList<>();
        for (T item : this) {
            result.add(item);
        }
        return result;
    }

    /**
     * Returns the first element of this query.
     *
     * <p>Short-circuits after reading the first available element.</p>
     *
     * @return the first element in the sequence
     * @throws NoSuchElementException if this query contains no elements
     */
    default T first() {
        Iterator<T> iterator = iterator();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException("Sequence contains no elements");
        }
        return iterator.next();
    }

    /**
     * Returns the first element of this query that satisfies {@code predicate}.
     *
     * <p>Short-circuits as soon as a matching element is found.</p>
     *
     * @param predicate the condition to match against elements in order
     * @return the first element matching {@code predicate}
     * @throws NullPointerException   if {@code predicate} is {@code null}
     * @throws NoSuchElementException if no element matches {@code predicate}
     */
    default T first(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return where(predicate).first();
    }

    /**
     * Returns the first element of this query wrapped in an {@link Optional}, or
     * {@link Optional#empty()} if the query has no elements.
     *
     * <p>Uses {@link Optional#ofNullable(Object)} so a {@code null} first element yields an
     * empty {@code Optional} rather than throwing {@link NullPointerException}.</p>
     *
     * @return an {@link Optional} describing the first element, or {@link Optional#empty()} if empty
     */
    default Optional<T> firstOrDefault() {
        try {
            return Optional.ofNullable(first());
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the first element of this query if present, or {@code defaultValue} if the query
     * contains no elements.
     *
     * @param defaultValue the value to return when this query is empty; may be {@code null}
     * @return the first element if present, otherwise {@code defaultValue}
     */
    default T firstOrDefault(T defaultValue) {
        Iterator<T> it = iterator();
        return it.hasNext() ? it.next() : defaultValue;
    }




    /**
     * Sorts the elements of this query in ascending order according to a comparable key.
     *
     * <p>Execution is deferred until the returned query is iterated, at which point elements are
     * materialized and sorted stably.</p>
     *
     * @param keySelector extracts the sort key from each element
     * @param <K>         the comparable key type
     * @return a new {@code Query} whose elements are sorted in ascending key order
     * @throws NullPointerException if {@code keySelector} is {@code null}
     */
    default <K extends Comparable<? super K>> Query<T> orderBy(
            Function<? super T, ? extends K> keySelector) {
        Objects.requireNonNull(keySelector, "keySelector");
        Comparator<T> comparator = Comparator.comparing(keySelector);
        return () -> new SortedIterable<>(this, comparator).iterator();
    }

    /**
     * Sorts the elements of this query in ascending order using the given {@link Comparator}.
     *
     * <p>Execution is deferred until the returned query is iterated.</p>
     *
     * @param comparator the comparator used to order elements
     * @return a new {@code Query} whose elements are sorted according to {@code comparator}
     * @throws NullPointerException if {@code comparator} is {@code null}
     */
    default Query<T> orderBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator");
        return () -> new SortedIterable<>(this, comparator).iterator();
    }

    /**
     * Sorts the elements of this query in descending order according to a comparable key.
     *
     * <p>Execution is deferred until the returned query is iterated, at which point elements are
     * materialized and sorted stably in reverse key order.</p>
     *
     * @param keySelector extracts the sort key from each element
     * @param <K>         the comparable key type
     * @return a new {@code Query} whose elements are sorted in descending key order
     * @throws NullPointerException if {@code keySelector} is {@code null}
     */
    default <K extends Comparable<? super K>> Query<T> orderByDescending(
            Function<? super T, ? extends K> keySelector) {
        Objects.requireNonNull(keySelector, "keySelector");
        Comparator<T> comparator = Comparator.<T, K>comparing(keySelector).reversed();
        return () -> new SortedIterable<>(this, comparator).iterator();
    }

    /**
     * Sorts the elements of this query in descending order using the given {@link Comparator}.
     *
     * <p>Execution is deferred until the returned query is iterated.</p>
     *
     * @param comparator the comparator defining ascending order before reversal
     * @return a new {@code Query} whose elements are sorted in reverse comparator order
     * @throws NullPointerException if {@code comparator} is {@code null}
     */
    default Query<T> orderByDescending(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator");
        return () -> new SortedIterable<>(this, comparator.reversed()).iterator();
    }



    /**
     * Returns the last element of this query.
     *
     * @return the last element in this sequence
     * @throws NoSuchElementException if this query contains no elements
     */
    default T last() {
        Iterator<T> iterator = iterator();

        if (!iterator.hasNext()) throw new NoSuchElementException("The sequence is empty");

        T lastValue = iterator.next();
        while (iterator.hasNext()) {
            lastValue = iterator.next();
        }
        return lastValue;
    }

    /**
     * Returns the last element of this query that satisfies {@code predicate}.
     *
     * @param predicate the condition each candidate element must satisfy
     * @return the last element matching {@code predicate}
     * @throws NullPointerException   if {@code predicate} is {@code null}
     * @throws NoSuchElementException if no element matches {@code predicate}
     */
    default T last(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return where(predicate).last();
    }

    /**
     * Returns the last element of this query wrapped in an {@link Optional}, or
     * {@link Optional#empty()} if the query contains no elements.
     *
     * <p>Uses {@link Optional#ofNullable(Object)} so a {@code null} last element produces an
     * empty {@code Optional} rather than throwing {@link NullPointerException}.</p>
     *
     * @return an {@link Optional} describing the last element, or {@link Optional#empty()} if empty
     */
    default Optional<T> lastOrDefault() {
        try {
            return Optional.ofNullable(last());
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the last element of this query if present, or {@code defaultValue} if the query
     * contains no elements.
     *
     * @param defaultValue the value to return when this query is empty; may be {@code null}
     * @return the last element if present, otherwise {@code defaultValue}
     */
    default T lastOrDefault(T defaultValue) {
        try {
            return last();
        } catch (NoSuchElementException e) {
            return defaultValue;
        }
    }
}