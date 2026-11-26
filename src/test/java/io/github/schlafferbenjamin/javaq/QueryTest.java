package io.github.schlafferbenjamin.javaq;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryTest {

    @Test
    void whereSelectTakeAndToListWorkLazily() {
        List<String> result = JQ.of(1, 2, 3, 4, 5, 6, 8)
                .where(n -> n % 2 == 0)
                .select(n -> "even-" + n)
                .take(3)
                .toList();

        assertEquals(
                List.of("even-2", "even-4", "even-6"),
                result
        );

        for (String item : result) {
            System.out.println(item);
        }
    }


    @Test
    void firstReturnsFirstElementAndThrowsOnEmpty() {
        assertEquals(10, JQ.of(10, 20, 30).first());
        assertEquals(20, JQ.of(10, 20, 30).first(n -> n > 15));
        assertThrows(NoSuchElementException.class, () -> JQ.<Integer>of().first());
        assertThrows(NoSuchElementException.class, () -> JQ.of(1, 2, 3).first(n -> n > 10));
    }

    @Test
    void firstOrDefaultHandlesEmptyAndFallback() {
        assertEquals(Optional.of("a"), JQ.of("a", "b").firstOrDefault());
        assertEquals(Optional.empty(), JQ.<String>of().firstOrDefault());
        assertEquals("fallback", JQ.<String>of().firstOrDefault("fallback"));
        assertNull(JQ.<String>of().firstOrDefault(null));
    }


    @Test
    void testBasics() {
        List<Integer> result = JQ.of(1,2,3,4,5,6,7,8,9,10)
                .where(x -> x > 5)
                .take(4)
                .orderByDescending(x -> x)
                .toList();

        System.out.println(result);
    }


    @Test
    void orderByAndOrderByDescendingSortLazilyAndStably() {
        List<String> items = List.of("b-1", "a-1", "b-2", "a-2");

        List<String> ascending = JQ.from(items)
                .orderBy(s -> s.substring(0, 1))
                .toList();
        assertEquals(List.of("a-1", "a-2", "b-1", "b-2"), ascending);

        List<String> descending = JQ.from(items)
                .orderByDescending(s -> s.substring(0, 1))
                .toList();
        assertEquals(List.of("b-1", "b-2", "a-1", "a-2"), descending);

        System.out.println(ascending);
        System.out.println(descending);
    }


    @Test
    void testOnListAlreadyExisting() {
        List<String> words = List.of("banana", "Apple", "Cherry");

        System.out.println(words);

        words = JQ.from(words)
                .orderBy(x -> x)
                .toList();

        System.out.println(words);
    }

    @Test
    void testSkips() {
        List<Integer> resultForward = JQ.of(1,2,3,4,5,6,7,8)
                .skip(3)
                .toList();

        List<Integer> resultBackwards = JQ.of(1,2,3,4,5,6,7,8)
                .skip(-3)
                .toList();

        assertEquals(
                List.of(4,5,6,7,8),
                resultForward
        );

        assertEquals(
                List.of(1,2,3,4,5),
                resultBackwards
        );

        assertEquals(
                List.of(4,5),
                JQ.of(1,2,3,4,5,6,7,8)
                        .skip(3)
                        .skip(-3)
                        .toList()
        );
    }
}