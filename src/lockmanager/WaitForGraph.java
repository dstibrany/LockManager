package lockmanager;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

class WaitForGraph {
    private ConcurrentHashMap<Transaction, ArrayList<Transaction>> adjacencyList = new ConcurrentHashMap<>();
    private static WaitForGraph graph = null;

    static WaitForGraph getInstance() {
        if (graph == null) {
            graph = new WaitForGraph();
        }
        return graph;
    }

    void addEdge(Transaction txn1, Transaction txn2) {
        ArrayList<Transaction> txnList = adjacencyList.getOrDefault(txn1, new ArrayList<>());
        txnList.add(txn2);
        adjacencyList.put(txn1, txnList);
    }

    boolean hasEdge(Transaction txn1, Transaction txn2) {
        ArrayList<Transaction> txnList = adjacencyList.get(txn1);
        if (txnList == null) return false;

        for (Transaction txn: txnList) {
            if (txn.equals(txn2)) return true;
        }

        return false;
    }

    void removeVertex(Transaction txn1) {
        adjacencyList.remove(txn1);
    }

    void removeEdge(Transaction txn1, Transaction txn2) {
        ArrayList<Transaction> txnList = adjacencyList.get(txn1);
        if (txnList == null) return;

        ArrayList<Transaction> newTxnList = new ArrayList<>();
        for (Transaction txn: txnList) {
            if (!txn.equals(txn2)) {
                newTxnList.add(txn);
            }
        }

        adjacencyList.put(txn1, newTxnList);
    }
}
