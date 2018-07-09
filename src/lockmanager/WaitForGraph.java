package lockmanager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class WaitForGraph {
    private ConcurrentHashMap<Transaction, HashSet<Transaction>> adjacencyList = new ConcurrentHashMap<>();
    private static WaitForGraph graph = null;

    // TODO: fix race condition on graph creation
    static WaitForGraph getInstance() {
        if (graph == null) {
            graph = new WaitForGraph();
        }
        return graph;
    }

    void add(Transaction head, Set<Transaction> tail) {
        HashSet<Transaction> txnList = adjacencyList.getOrDefault(head, new HashSet<>());
        txnList.addAll(tail);
        adjacencyList.put(head, txnList);
    }

    void remove(Transaction txn) {
        HashSet<Transaction> txnList = adjacencyList.get(txn);
        if (txnList == null) return;

//        HashSet<Transaction> newTxnList = new HashSet<>();
//        for (Transaction txn: txnList) {
//            if (!txn.equals(txn2)) {
//                newTxnList.add(txn);
//            }
//        }
//
//        adjacencyList.put(txn1, newTxnList);
    }

    boolean hasEdge(Transaction txn1, Transaction txn2) {
        HashSet<Transaction> txnList = adjacencyList.get(txn1);
        if (txnList == null) return false;

        for (Transaction txn: txnList) {
            if (txn.equals(txn2)) return true;
        }

        return false;
    }
}
