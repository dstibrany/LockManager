package lockmanager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void addEdge() {
        graph.addEdge(txn1, txn2);
        assertTrue(graph.hasEdge(txn1, txn2));
        assertFalse(graph.hasEdge(txn2, txn1));
        assertFalse(graph.hasEdge(txn1, txn3));
        assertFalse(graph.hasEdge(txn3, txn1));
    }

    @Test
    void removeVertex() {
        graph.addEdge(txn1, txn2);
        graph.removeVertex(txn1);
        assertFalse(graph.hasEdge(txn1, txn2));
    }

    @Test
    void removeEdge() {
        graph.addEdge(txn1, txn2);
        graph.removeEdge(txn1, txn2);
        assertFalse(graph.hasEdge(txn1, txn2));
    }
}