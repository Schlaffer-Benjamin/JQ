package io.github.schlafferbenjamin.javaq;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QueryCoreOperatorsTest {

    @Test
    void testSelectManyFlattensAndSkipsEmptyInnerIterables() {
        List<String> flattened = JQ.<List<String>>of(
                        List.of("a", "b"),
                        List.of(),
                        List.of("c"),
                        List.of("d", "e")
                )
                .selectMany(list -> list)
                .toList();

        assertEquals(List.of("a", "b", "c", "d", "e"), flattened);
    }

    @Test
    void testSelectManyEmptyAndNullGuards() {
        assertEquals(List.of(), JQ.<List<Integer>>of().selectMany(list -> list).toList());
        assertThrows(NullPointerException.class, () -> JQ.of(List.of(1)).selectMany(null));
        assertThrows(NullPointerException.class, () ->
                JQ.of(1, 2).selectMany(x -> null).toList()
        );
    }

    @Test
    void testCount() {
        assertEquals(0, JQ.of().count());
        assertEquals(4, JQ.of(10, 20, 30, 40).count());
        assertEquals(2, JQ.of(1, 2, 3, 4, 5).where(n -> n % 2 == 0).count());
    }

    @Test
    void testDistinctPreservesFirstSeenOrderAndHandlesEmpty() {
        List<Integer> distinctItems = JQ.of(3, 1, 2, 3, 2, 4, 1)
                .distinct()
                .toList();

        assertEquals(List.of(3, 1, 2, 4), distinctItems);
        assertEquals(List.of(), JQ.<Integer>of().distinct().toList());
    }

    @Test
    void testToSetPreservesEncounterOrderAndDeduplicates() {
        Set<String> result = JQ.of("b", "a", "b", "c", "a")
                .toSet();

        assertEquals(Set.of("a", "b", "c"), result);
        assertEquals(List.of("b", "a", "c"), List.copyOf(result));
        assertTrue(JQ.of().toSet().isEmpty());
    }
}