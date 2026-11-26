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
}