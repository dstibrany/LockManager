package com.dstibrany.lockmanager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Lock {
    private final Set<Transaction> owners = new HashSet<>();
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition waiters = lock.newCondition();
    private final WaitForGraph waitForGraph;
    private int xLockCount = 0;
    private int sLockCount = 0;

    Lock(WaitForGraph waitForGraph) {
        this.waitForGraph = waitForGraph;
    }

    void acquire(Transaction txn, LockMode lockMode) throws InterruptedException {
        if (lockMode == LockMode.SHARED) {
            acquireSLock(txn);
        } else if (lockMode == LockMode.EXCLUSIVE) {
            acquireXLock(txn);
        } else {
            throw new RuntimeException("Lock mode does not exist");
        }
    }

    void release(Transaction txn) {
        lock.lock();
        try {
            if (sLockCount > 0) {
                sLockCount--;
            }
            if (xLockCount == 1) {
                xLockCount = 0;
            }

            owners.remove(txn);
            waitForGraph.remove(txn);

            waiters.signalAll();
        } finally {
            lock.unlock();
        }
    }

    void upgrade(Transaction txn) throws InterruptedException {
        lock.lock();
        try {
            if (owners.contains(txn) && isXLocked()) return;
            while (isXLocked() || sLockCount > 1) {
                Set<Transaction> ownersWithSelfRemoved = owners.stream().filter((ownerTxn) -> !ownerTxn.equals(txn)).collect(Collectors.toSet());
                waitForGraph.add(txn, ownersWithSelfRemoved);
                waitForGraph.detectDeadlock(txn);
                waiters.await();
            }
            sLockCount = 0;
            xLockCount = 1;
        } finally {
            lock.unlock();
        }
    }

    LockMode getMode() {
        LockMode lockMode = null;
        lock.lock();

        try {
            if (isXLocked()) lockMode = LockMode.EXCLUSIVE;
            else if (isSLocked()) lockMode = LockMode.SHARED;
        } finally {
            lock.unlock();
        }

        return lockMode;
    }

    Set<Transaction> getOwners() {
        return owners;
    }

    private void acquireSLock(Transaction txn) throws InterruptedException {
        lock.lock();
        try {
            while (isXLocked() || lock.hasWaiters(waiters)) {
                waitForGraph.add(txn, owners);
                waitForGraph.detectDeadlock(txn);
                waiters.await();
            }
            sLockCount++;
            owners.add(txn);
        } finally {
            lock.unlock();
        }
    }

    private void acquireXLock(Transaction txn) throws InterruptedException {
        lock.lock();
        try {
            while (isXLocked() || isSLocked()) {
                waitForGraph.add(txn, owners);
                waitForGraph.detectDeadlock(txn);
                waiters.await();
            }
            xLockCount = 1;
            owners.add(txn);
        } finally {
            lock.unlock();
        }
    }

    private boolean isXLocked() {
        return xLockCount == 1;
    }

    private boolean isSLocked() {
        return sLockCount > 0;
    }

    public enum LockMode {
        SHARED,
        EXCLUSIVE
    }

}
