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
     * Projects each element of this query to an {@link Iterable} and flattens the resulting
     * sequences into one lazy query.
     *
     * <p>Evaluation is lazy: inner iterables are requested and iterated only as elements are
     * consumed.</p>
     *
     * @param selector a function mapping each element to a non-null {@link Iterable}
     * @param <R>      the element type of the flattened sequence
     * @return a new lazy {@code Query} containing all elements from each inner iterable in order
     * @throws NullPointerException if {@code selector} is {@code null}, or if {@code selector}
     *                              returns {@code null} for an element
     */
    default <R> Query<R> selectMany(Function<? super T, ? extends Iterable<? extends R>> selector) {
        Objects.requireNonNull(selector, "selector");
        return () -> new AbstractIterator<R>() {
            private final Iterator<T> source = iterator();
            private Iterator<? extends R> currentInner = Collections.emptyIterator();

            @Override
            protected R computeNext() {
                while (!currentInner.hasNext()) {
                    if (!source.hasNext()) {
                        return endOfData();
                    }
                    Iterable<? extends R> nextIterable = selector.apply(source.next());
                    Objects.requireNonNull(nextIterable, "selector must not return null");
                    currentInner = nextIterable.iterator();
                }
                return currentInner.next();
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


    /**
     * Skips {@code count} elements from the start when {@code count >= 0}, or skips
     * {@code |count|} elements from the end when {@code count < 0}.
     *
     * @param count number of elements to skip from the start (if non-negative) or from the end
     *              (if negative)
     * @return a lazy query with the specified leading or trailing elements skipped
     */
    default Query<T> skip(int count) {
        if (count >= 0) {
            return skipFromStart(count);
        }
        int dropFromEnd = (count == Integer.MIN_VALUE) ? Integer.MAX_VALUE : -count;
        return skipFromEnd(dropFromEnd);
    }

    private Query<T> skipFromStart(int count) {
        return () -> new AbstractIterator<T>() {
            private Iterator<T> source;
            private int remainingToSkip = count;

            @Override
            protected T computeNext() {
                if (source == null) {
                    source = iterator();
                }

                while (remainingToSkip > 0 && source.hasNext()) {
                    source.next();
                    remainingToSkip--;
                }

                if (!source.hasNext()) {
                    return endOfData();
                }

                return source.next();
            }
        };
    }

    private Query<T> skipFromEnd(int dropFromEnd) {
        return () -> new AbstractIterator<T>() {
            private List<T> buffer;
            private int index;

            @Override
            protected T computeNext() {
                if (buffer == null) {
                    buffer = new ArrayList<>();
                    Iterator<T> it = iterator();
                    while (it.hasNext()) {
                        buffer.add(it.next());
                    }
                }

                int keepUntil = buffer.size() - dropFromEnd;
                if (index >= keepUntil) {
                    return endOfData();
                }

                return buffer.get(index++);
            }
        };
    }




    /**
     * Determines whether this query contains any elements.
     *
     * @return {@code true} if this query contains at least one element; {@code false} otherwise
     */
    default boolean any() {
        return iterator().hasNext();
    }

    /**
     * Determines whether any element of this query satisfies {@code predicate}.
     *
     * <p>Evaluation stops as soon as the first matching element is found.</p>
     *
     * @param predicate the condition to test each element against
     * @return {@code true} if at least one element matches {@code predicate}; {@code false}
     *         otherwise (including when this query is empty)
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    default boolean any(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        Iterator<T> iterator = iterator();
        while (iterator.hasNext()) {
            if (predicate.test(iterator.next())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Determines whether all elements of this query satisfy {@code predicate}.
     *
     * <p>Evaluation stops as soon as an element fails {@code predicate}. If this query is empty,
     * returns {@code true}.</p>
     *
     * @param predicate the condition every element must satisfy
     * @return {@code true} if every element matches {@code predicate} or the query is empty;
     *         {@code false} if any element does not match
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    default boolean all(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        Iterator<T> iterator = iterator();

        while (iterator.hasNext()) {
            if (!predicate.test(iterator.next())) return false;
        }

        return true;
    }


    /**
     * Counts the elements inside a sequence
     * @return The size of the given sequence
     */
    default int count() {
        int count = 0;
        Iterator<T> iterator = iterator();
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    /**
     *
     * @return Gives back the sequence without any duplicates
     */
    default Query<T> distinct() {
        return () -> new AbstractIterator<>() {
            private final Iterator<T> source = iterator();
            private final Set<T> seen = new HashSet<>();

            @Override
            protected T computeNext() {
                while (source.hasNext()) {
                    T item = source.next();
                    if (seen.add(item)) {
                        return item;
                    }
                }
                return endOfData();
            }
        };
    }


    /**
     * Creates a set from the sequence it is called on
     * @return A set made from the given sequence
     */
    default Set<T> toSet() {
        Set<T> result = new LinkedHashSet<>();
        for (T item : this) {
            result.add(item);
        }
        return result;
    }

}