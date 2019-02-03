package com.dstibrany.lockmanager;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LockManager {
    private ConcurrentHashMap<Integer, Lock> lockTable = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Transaction> txnTable = new ConcurrentHashMap<>();
    private WaitForGraph waitForGraph;

    public LockManager() {
        waitForGraph = new WaitForGraph();
    }

    public LockManager(int deadlockDetectorInitialDelay, int deadlockDetectorDelay) {
        waitForGraph = new WaitForGraph(deadlockDetectorInitialDelay, deadlockDetectorDelay);
    }

    public void lock(int lockName, int txnId, Lock.LockMode requestedMode) throws DeadlockException {
        lockTable.putIfAbsent(lockName, new Lock(waitForGraph));
        Lock lock = lockTable.get(lockName);
        Transaction txn = txnTable.getOrDefault(txnId, new Transaction(txnId));

        try {
            if (requestedMode == Lock.LockMode.SHARED && hasLock(txnId, lockName) && lock.getMode() == Lock.LockMode.SHARED) {
                return;
            } else if (requestedMode == Lock.LockMode.EXCLUSIVE && hasLock(txnId, lockName) && lock.getMode() == Lock.LockMode.EXCLUSIVE) {
                return;
            } else if (requestedMode == Lock.LockMode.EXCLUSIVE && hasLock(txnId, lockName) && lock.getMode() == Lock.LockMode.SHARED) {
                lock.upgrade(txn);
            } else {
                lock.acquire(txn, requestedMode);
            }
        } catch (InterruptedException e) {
            removeTransaction(txnId);
            throw new DeadlockException(e);
        }

        txn.addLock(lock);
        txnTable.putIfAbsent(txnId, txn);
    }

    public void unlock(int lockName, int txnId) {
        Transaction txn = txnTable.get(txnId);
        Lock lock = lockTable.get(lockName);
        if (lock != null) {
            lock.release(txn);
        }
        txn.removeLock(lock);
    }

    public void removeTransaction(int txnId) {
        Transaction txn = txnTable.get(txnId);
        if (txn == null) return;
        List<Lock> txnLockList = txn.getLocks();

        for (Lock lock : txnLockList) {
            lock.release(txn);
        }

        txnTable.remove(txnId);
    }

    // TODO: does this work concurrently?
    public boolean hasLock(int txnId, int lockName) {
        Transaction txn = txnTable.get(txnId);
        if (txn == null) return false;
        List<Lock> lockList = txn.getLocks();
        if (lockList == null) return false;

        boolean found = false;

        for (Lock txnLock : lockList) {
            if (txnLock == lockTable.get(lockName)) found = true;
        }

        return found;
    }

    Lock.LockMode getLockMode(Integer lockName) {
        return lockTable.get(lockName).getMode();
    }

    WaitForGraph getWaitForGraph() {
        return waitForGraph;
    }
}

