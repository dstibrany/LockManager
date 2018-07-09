package lockmanager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class WaitForGraphTest {
    private WaitForGraph graph;
    private Transaction txn1 = new Transaction(1);
    private Transaction txn2 = new Transaction(2);
    private Transaction txn3 = new Transaction(3);

    @BeforeEach
    void setUp() {
        graph = new WaitForGraph();
    }

    @Test
    void add() {
        graph.add(txn1, new HashSet<>(Arrays.asList(txn2, txn3)));
        assertTrue(graph.hasEdge(txn1, txn2));
        assertFalse(graph.hasEdge(txn2, txn1));
        assertTrue(graph.hasEdge(txn1, txn3));
        assertFalse(graph.hasEdge(txn3, txn1));
    }

    @Test
    void remove() {
        graph.add(txn1, new HashSet<>(Collections.singletonList(txn2)));
        graph.remove(txn1);
        assertFalse(graph.hasEdge(txn1, txn2));
    }
}