package lockmanager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

class WaitForGraph {
    private final ConcurrentMap<Transaction, Set<Transaction>> adjacencyList = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock sharedLock = rwl.readLock();
    private final Lock exclusiveLock = rwl.readLock();


    void add(Transaction predecessor, Set<Transaction> successors) {
        sharedLock.lock();
        try {
            Set<Transaction> txnList = adjacencyList.getOrDefault(predecessor, new ConcurrentSkipListSet<>());
            txnList.addAll(successors);
            adjacencyList.put(predecessor, txnList);
        } finally {
            sharedLock.unlock();
        }
    }

    void remove(Transaction txn) {
        sharedLock.lock();
        try {
            adjacencyList.remove(txn);
            removeSuccessor(txn);
        } finally {
            sharedLock.unlock();
        }
    }

    boolean hasEdge(Transaction txn1, Transaction txn2) {
        Set<Transaction> txnList = adjacencyList.get(txn1);
        if (txnList == null) return false;
        return txnList.contains(txn2);
    }

    ScheduledFuture<?> startDetectionLoop(int initialDelay, int delay, TimeUnit timeUnit) {
        ScheduledExecutorService es = newSingleThreadScheduledExecutor();
        return es.scheduleWithFixedDelay(() -> {
            List<List<Transaction>> cycles = findCycles();
            for (List<Transaction> cycleGroup: cycles) {
                for (Transaction t: cycleGroup) {
                    t.abort();
                }
            }
        }, initialDelay, delay, timeUnit);
    }

    List<List<Transaction>> findCycles() {
        exclusiveLock.lock();
        try {
            DepthFirstSearch dfs = new DepthFirstSearch();
            dfs.start();
            return dfs.getCycles();
        } finally {
            exclusiveLock.unlock();
        }
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
