package lockmanager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class WaitForGraphTest {
    private WaitForGraph graph;
    private Transaction txn1;
    private Transaction txn2;
    private Transaction txn3;

    @BeforeEach
    void setUp() {
        graph = new WaitForGraph();
        txn1 = new Transaction(1);
        txn2 = new Transaction(2);
        txn3 = new Transaction(3);
    }

    @Test
    void add() {
        graph.add(txn1, new HashSet<>(Arrays.asList(txn2, txn3)));
        assertTrue(graph.hasEdge(txn1, txn2));
        assertTrue(graph.hasEdge(txn1, txn3));
        assertFalse(graph.hasEdge(txn2, txn1));
        assertFalse(graph.hasEdge(txn3, txn1));
        assertFalse(graph.hasEdge(txn2, txn3));
        assertFalse(graph.hasEdge(txn3, txn2));
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
    void dfsConnectedGraph() {
        ArrayList<Transaction> txnList = new ArrayList<>();
        for (int i = 0; i <= 9; i++) {
            txnList.add(new Transaction(i));
        }
        graph.add(txnList.get(1), new HashSet<>(Arrays.asList(txnList.get(2), txnList.get(3))));
        graph.add(txnList.get(2), new HashSet<>(Arrays.asList(txnList.get(4), txnList.get(5))));
        graph.add(txnList.get(5), new HashSet<>(Arrays.asList(txnList.get(6), txnList.get(7))));
        graph.add(txnList.get(7), new HashSet<>(Arrays.asList(txnList.get(8), txnList.get(9))));
        graph.add(txnList.get(0), new HashSet<>(Arrays.asList(txnList.get(1))));

        WaitForGraph.DepthFirstSearch dfs = graph.new DepthFirstSearch();
        dfs.start();
        Set<Transaction> discoveredTxns = dfs.getVisited();

        assertArrayEquals(txnList.stream().map(Transaction::getId).sorted().toArray(),
                discoveredTxns.stream().map(Transaction::getId).sorted().toArray());
    }

    @Test
    void dfsNonConnectedGraph() {
        ArrayList<Transaction> txnList = new ArrayList<>();
        for (int i = 0; i <= 9; i++) {
            txnList.add(new Transaction(i));
        }
        graph.add(txnList.get(1), new HashSet<>(Arrays.asList(txnList.get(2), txnList.get(3))));
        graph.add(txnList.get(2), new HashSet<>(Arrays.asList(txnList.get(4), txnList.get(5))));
        graph.add(txnList.get(6), new HashSet<>(Arrays.asList(txnList.get(7))));
        graph.add(txnList.get(7), new HashSet<>(Arrays.asList(txnList.get(8), txnList.get(9))));
        graph.add(txnList.get(0), new HashSet<>(Arrays.asList(txnList.get(1))));

        WaitForGraph.DepthFirstSearch dfs = graph.new DepthFirstSearch();
        dfs.start();
        Set<Transaction> discoveredTxns = dfs.getVisited();

        assertArrayEquals(txnList.stream().map(Transaction::getId).sorted().toArray(),
                discoveredTxns.stream().map(Transaction::getId).sorted().toArray());
    }

    @Test
    void dfsWithCycle() {
        ArrayList<Transaction> txnList = new ArrayList<>();
        for (int i = 0; i <= 2; i++) {
            txnList.add(new Transaction(i));
        }
        graph.add(txnList.get(0), new HashSet<>(Arrays.asList(txnList.get(1))));
        graph.add(txnList.get(1), new HashSet<>(Arrays.asList(txnList.get(2))));
        graph.add(txnList.get(2), new HashSet<>(Arrays.asList(txnList.get(0))));

        WaitForGraph.DepthFirstSearch dfs = graph.new DepthFirstSearch();
        dfs.start();
        Set<Transaction> discoveredTxns = dfs.getVisited();

        assertArrayEquals(txnList.stream().map(Transaction::getId).sorted().toArray(),
                discoveredTxns.stream().map(Transaction::getId).sorted().toArray());
    }

    @Test
    void noCycle() {
        ArrayList<Transaction> txnList = new ArrayList<>();
        for (int i = 0; i <= 2; i++) {
            txnList.add(new Transaction(i));
        }
        graph.add(txnList.get(0), new HashSet<>(Arrays.asList(txnList.get(1), txnList.get(2))));
        graph.add(txnList.get(1), new HashSet<>(Arrays.asList(txnList.get(2))));

        List<List<Transaction>> cycles = graph.findCycles();
        assertTrue(cycles.isEmpty());
    }

    @Test
    void simpleCycle() {
        ArrayList<Transaction> txnList = new ArrayList<>();
        for (int i = 0; i <= 2; i++) {
            txnList.add(new Transaction(i));
        }
        graph.add(txnList.get(0), new HashSet<>(Arrays.asList(txnList.get(1))));
        graph.add(txnList.get(1), new HashSet<>(Arrays.asList(txnList.get(2))));
        graph.add(txnList.get(2), new HashSet<>(Arrays.asList(txnList.get(0))));

        List<List<Transaction>> cycles = graph.findCycles();

        assertEquals(1, cycles.size());
        List<Transaction> cycle = cycles.get(0);
        assertEquals(3, cycle.size());
        assertArrayEquals(txnList.stream().map(Transaction::getId).sorted().toArray(),
                cycle.stream().map(Transaction::getId).sorted().toArray());
    }

    @Test
    void complexCycle() {
        ArrayList<Transaction> txnList = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            txnList.add(new Transaction(i));
        }
        graph.add(txnList.get(0), new HashSet<>(Arrays.asList(txnList.get(1))));
        graph.add(txnList.get(1), new HashSet<>(Arrays.asList(txnList.get(2))));
        graph.add(txnList.get(2), new HashSet<>(Arrays.asList(txnList.get(3))));
        graph.add(txnList.get(3), new HashSet<>(Arrays.asList(txnList.get(4))));
        graph.add(txnList.get(4), new HashSet<>(Arrays.asList(txnList.get(2))));

        List<List<Transaction>> cycles = graph.findCycles();

        assertEquals(1, cycles.size());
        List<Transaction> cycle = cycles.get(0);
        assertEquals(3, cycle.size());
        assertArrayEquals(Stream.of(2, 3, 4).toArray(),
                cycle.stream().map(Transaction::getId).sorted().toArray());
    }

    @Test
    void multipleCycles() {
        ArrayList<Transaction> txnList = new ArrayList<>();
        for (int i = 0; i <= 6; i++) {
            txnList.add(new Transaction(i));
        }
        graph.add(txnList.get(0), new HashSet<>(Arrays.asList(txnList.get(1))));
        graph.add(txnList.get(1), new HashSet<>(Arrays.asList(txnList.get(2))));
        graph.add(txnList.get(2), new HashSet<>(Arrays.asList(txnList.get(3))));
        graph.add(txnList.get(3), new HashSet<>(Arrays.asList(txnList.get(2))));
        graph.add(txnList.get(1), new HashSet<>(Arrays.asList(txnList.get(4))));
        graph.add(txnList.get(4), new HashSet<>(Arrays.asList(txnList.get(5))));
        graph.add(txnList.get(5), new HashSet<>(Arrays.asList(txnList.get(6))));
        graph.add(txnList.get(6), new HashSet<>(Arrays.asList(txnList.get(4))));

        List<List<Transaction>> cycles = graph.findCycles();

        assertEquals(2, cycles.size());

        List<Transaction> cycle1 = cycles.get(0);
        List<Transaction> cycle2 = cycles.get(1);

        assertEquals(2, cycle1.size());
        assertArrayEquals(Stream.of(2, 3).toArray(),
                cycle1.stream().map(Transaction::getId).sorted().toArray());
        assertEquals(3, cycle2.size());
        assertArrayEquals(Stream.of(4, 5, 6).toArray(),
                cycle2.stream().map(Transaction::getId).sorted().toArray());
    }

    @Test
    void detectionLoopAbortsTransactions() {
        assertThrows(InterruptedException.class, () -> {
            WaitForGraph graph = new WaitForGraph(200, 5000);
            ArrayList<Transaction> txnList = new ArrayList<>();
            for (int i = 0; i <= 1; i++) {
                txnList.add(new Transaction(i));
            }
            graph.add(txnList.get(0), new HashSet<>(Arrays.asList(txnList.get(1))));
            graph.add(txnList.get(1), new HashSet<>(Arrays.asList(txnList.get(0))));

            Thread.sleep(1000);
        }, "Deadlock detector did abort any transactions");
    }
}