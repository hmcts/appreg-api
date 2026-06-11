package uk.gov.hmcts.appregister.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReadOnlyListTest {

    @Test
    void readOnlyList_delegatesReadOperations() {
        var list = new ReadOnlyList<>(List.of("one", "two"));

        assertEquals(2, list.size());
        assertEquals("one", list.get(0));
        assertEquals("two", list.get(1));
    }

    @Test
    void readOnlyList_rejectsMutatingOperations() {
        var list = new ReadOnlyList<>(new ArrayList<>(List.of("one", "two")));

        assertThrows(IllegalArgumentException.class, () -> list.replaceAll(String::toUpperCase));
        assertThrows(IllegalArgumentException.class, () -> list.addFirst("zero"));
        assertThrows(IllegalArgumentException.class, () -> list.addLast("three"));
        assertThrows(IllegalArgumentException.class, list::removeFirst);
        assertThrows(IllegalArgumentException.class, list::removeLast);
        assertThrows(
                IllegalArgumentException.class,
                () -> list.removeIf(value -> value.startsWith("o")));
    }
}
