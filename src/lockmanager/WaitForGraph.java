package lockmanager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

class WaitForGraph {
    private ConcurrentMap<Transaction, Set<Transaction>> adjacencyList = new ConcurrentHashMap<>();

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

    List<List<Transaction>> findDeadlocks() {
        DepthFirstSearch dfs = new DepthFirstSearch();
        dfs.start();
        return dfs.getCycles();
    }

    private void removeSuccessor(Transaction txnToRemove) {
        for (Transaction predecessor: adjacencyList.keySet()) {
            Set<Transaction> successors = adjacencyList.get(predecessor);
            successors.remove(txnToRemove);
        }
    }

    class DepthFirstSearch {
        private Set<Transaction> visited = new HashSet<>();
        private List<List<Transaction>> cycles = new ArrayList<>();

        void start() {
            for (Transaction txn: adjacencyList.keySet()) {
                if (!visited.contains(txn)) {
                    visit(txn, new ArrayList<>());
                }
            }
        }

        List<List<Transaction>> getCycles() {
            return cycles;
        }

        Set<Transaction> getVisited() {
            return visited;
        }

        private void visit(Transaction node, List<Transaction> path) {
            visited.add(node);
            path.add(node);

            if (adjacencyList.containsKey(node)) {
                for (Transaction neighbour: adjacencyList.get(node)) {
                    if (!visited.contains(neighbour)) {
                        visit(neighbour, new ArrayList<>(path));
                    } else {
                        if (path.contains(neighbour)) {
                            cycles.add(getCycleFromPath(path, neighbour));
                        }
                    }
                }
            }
        }

        private List<Transaction> getCycleFromPath(List<Transaction> path, Transaction target) {
            return path.subList(path.indexOf(target), path.size());
        }
    }
}
