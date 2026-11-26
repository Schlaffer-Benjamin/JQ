package io.github.schlafferbenjamin.javaq;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryTest {

    @Test
    void whereSelectTakeAndToListWorkLazily() {
        List<String> result = JQ.of(1, 2, 3, 4, 5, 6, 8)
                .where(n -> n % 2 == 0)
                .select(n -> "even-" + n)
                .take(2)
                .toList();

        assertEquals(
                List.of("even-2", "even-4", "even-6"),
                result
        );

        for (String item : result) {
            System.out.println(item);
        }
    }
}