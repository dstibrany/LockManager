package lockmanager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

class WaitForGraph {
    ConcurrentHashMap<Transaction, Set<Transaction>> adjacencyList = new ConcurrentHashMap<>();

    void add(Transaction predecessor, Set<Transaction> successors) {
        Set<Transaction> txnList = adjacencyList.getOrDefault(predecessor, new ConcurrentSkipListSet<>());
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
