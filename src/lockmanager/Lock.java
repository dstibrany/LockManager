package lockmanager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class Lock extends ReentrantLock{
    private int xLockCount = 0;
    private int sLockCount = 0;
    private Thread xOwner;
    private Set<Transaction> owners = new HashSet<>();
    final private ReentrantLock lock = new ReentrantLock(true);
    final private Condition waiters  = lock.newCondition();

    void acquire(Transaction txn, String mode) throws InterruptedException {
        if (xOwner == Thread.currentThread()) return;

        if ("S".equals(mode)) {
            acquireSLock(txn);
        } else if ("X".equals(mode)) {
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
                xOwner = null;
            }
            owners.remove(txn);
            waiters.signalAll();
        } finally {
            lock.unlock();
        }
    }

    void upgrade(Transaction txn) throws InterruptedException {
        lock.lock();
        try {
            if (xOwner == Thread.currentThread()) return;
            while (isXLocked() || sLockCount > 1) {
                // add to wait graph
                waiters.await();
            }
            sLockCount = 0;
            xLockCount = 1;
        } finally {
            lock.unlock();
        }
    }

    String getMode() {
        String mode = null;
        lock.lock();

        try {
            if (isXLocked()) mode =  "X";
            else if (isSLocked()) mode = "S";
        } finally {
            lock.unlock();
        }

        return mode;
    }

    Set<Transaction> getOwners() {
        return owners;
    }

    private void acquireSLock(Transaction txn) throws InterruptedException {
        lock.lock();
        try {
            while (isXLocked() || lock.hasWaiters(waiters)) {
                // add to wait graph
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
                // add to wait graph
                waiters.await();
            }
            xLockCount = 1;
            owners.add(txn);
            xOwner = Thread.currentThread();
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
}
