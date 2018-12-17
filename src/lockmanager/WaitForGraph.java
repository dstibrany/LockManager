package lockmanager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class WaitForGraph {
    ConcurrentHashMap<Transaction, Set<Transaction>> adjacencyList = new ConcurrentHashMap<>();
    private static WaitForGraph graph = null;

    synchronized static WaitForGraph getInstance() {
        if (graph == null) {
            graph = new WaitForGraph();
        }
        return graph;
    }

    synchronized static void reset() {
        graph = new WaitForGraph();
    }

    void add(Transaction predecessor, Set<Transaction> successors) {
        Set<Transaction> txnList = adjacencyList.getOrDefault(predecessor, new HashSet<>());
        txnList.addAll(successors);
        adjacencyList.put(predecessor, txnList);
    }

    void remove(Transaction txn) {
        adjacencyList.remove(txn);
        removeSuccessor(txn);
    }

    boolean hasEdge(Transaction txn1, Transaction txn2) {
        Set<Transaction> txnList = adjacencyList.get(txn1);
        if (txnList == null) return false;

        return txnList.contains(txn2);
    }

    private void removeSuccessor(Transaction txnToRemove) {
        for (Transaction predecessor: adjacencyList.keySet()) {
            Set<Transaction> successors = adjacencyList.get(predecessor);
            successors.remove(txnToRemove);
        }
    }
}
