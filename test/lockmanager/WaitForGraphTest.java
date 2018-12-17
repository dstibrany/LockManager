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
        assertFalse(graph.hasEdge(txn2, txn3));
    }

    @Test
    void removePredecessor() {
        graph.add(txn1, new HashSet<>(Arrays.asList(txn2, txn3)));
        graph.remove(txn1);
        assertFalse(graph.hasEdge(txn1, txn2));
        assertFalse(graph.hasEdge(txn1, txn3));
    }

    @Test
    void removeSuccessor() {
        graph.add(txn1, new HashSet<>(Arrays.asList(txn2, txn3)));
        graph.remove(txn2);
        assertFalse(graph.hasEdge(txn1, txn2));
        assertTrue(graph.hasEdge(txn1, txn3));
    }

    @Test
    void removeSuccessorAndPredecessor() {
        graph.add(txn1, new HashSet<>(Arrays.asList(txn2, txn3)));
        graph.add(txn2, new HashSet<>(Arrays.asList(txn3)));
        graph.remove(txn2);
        assertFalse(graph.hasEdge(txn1, txn2));
        assertFalse(graph.hasEdge(txn2, txn3));
        assertTrue(graph.hasEdge(txn1, txn3));
    }

    @Test
    void x() throws Throwable {
        for (int i = 0; i < 10000; i++) {
            Transaction txn1 = new Transaction(1);
            Transaction txn2 = new Transaction(2);
            Transaction txn3 = new Transaction(3);
            Transaction txn4 = new Transaction(4);
            Transaction txn5 = new Transaction(5);
            Transaction txn6 = new Transaction(6);
            Transaction txn7 = new Transaction(7);
            Transaction txn8 = new Transaction(8);
//            graph.add(txn1, new HashSet<>(Arrays.asList(txn2, txn3, txn4, txn5, txn6, txn7, txn8)));


            Thread t2 = new Thread(() -> {
                graph.add(txn1, new HashSet<>(Arrays.asList(txn2)));
                graph.remove(txn2);
            });
            Thread t3 = new Thread(() -> {
                graph.add(txn1, new HashSet<>(Arrays.asList(txn3)));
                graph.remove(txn3);
            });
            Thread t4 = new Thread(() -> {
                graph.add(txn1, new HashSet<>(Arrays.asList(txn4)));
                graph.remove(txn4);
            });
            Thread t5 = new Thread(() -> {
                graph.add(txn1, new HashSet<>(Arrays.asList(txn5)));
                graph.remove(txn5);
            });
            Thread t6 = new Thread(() -> {
                graph.add(txn1, new HashSet<>(Arrays.asList(txn6)));
                graph.remove(txn6);
            });
            Thread t7 = new Thread(() -> {
                graph.add(txn1, new HashSet<>(Arrays.asList(txn7)));
                graph.remove(txn7);
            });
            Thread t8 = new Thread(() -> {
                graph.add(txn1, new HashSet<>(Arrays.asList(txn8)));
                graph.remove(txn8);
            });

            t2.start();
            t3.start();
            t4.start();
            t5.start();
            t6.start();
            t7.start();
            t8.start();
            t2.join();
            t3.join();
            t4.join();
            t5.join();
            t6.join();
            t7.join();
            t8.join();

            assertFalse(graph.hasEdge(txn1, txn2));
            assertFalse(graph.hasEdge(txn1, txn3));
            assertFalse(graph.hasEdge(txn1, txn4));
            assertFalse(graph.hasEdge(txn1, txn5));
            assertFalse(graph.hasEdge(txn1, txn6));
            assertFalse(graph.hasEdge(txn1, txn7));
            assertFalse(graph.hasEdge(txn1, txn8));


        }


    }
}