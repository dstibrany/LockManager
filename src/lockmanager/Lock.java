package lockmanager;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class Lock extends ReentrantLock{
    private int xLockCount = 0;
    private int sLockCount = 0;
    private Thread xOwner;
    final private ReentrantLock lock = new ReentrantLock(true);
    final private Condition waiters  = lock.newCondition();

    void acquire(String mode) throws InterruptedException {
        if (xOwner == Thread.currentThread()) return;

        if ("S".equals(mode)) {
            acquireSLock();
        } else if ("X".equals(mode)) {
            acquireXLock();
        } else {
            throw new RuntimeException("Lock mode does not exist");
        }
    }

    void release() {
        lock.lock();
        try {
            if (sLockCount > 0) {
                sLockCount--;
            }
            if (xLockCount == 1) {
                xLockCount = 0;
                xOwner = null;
            }
            waiters.signalAll();
        } finally {
            lock.unlock();
        }
    }

    void upgrade() throws InterruptedException {
        lock.lock();
        try {
            if (xOwner == Thread.currentThread()) return;
            while (isXLocked() || sLockCount > 1) {
                waiters.await();
            }
            sLockCount = 0;
            xLockCount = 1;
        } finally {
            lock.unlock();
        }
    }

    private void acquireSLock() throws InterruptedException {
        lock.lock();
        try {
            while (isXLocked() || lock.hasWaiters(waiters)) {
                waiters.await();
            }
            sLockCount++;
        } finally {
            lock.unlock();
        }
    }

    private void acquireXLock() throws InterruptedException {
        lock.lock();
        try {
            while (isXLocked() || isSLocked()) {
                waiters.await();
            }
            xLockCount = 1;
            xOwner = Thread.currentThread();
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

    private boolean isXLocked() {
        return xLockCount == 1;
    }

    private boolean isSLocked() {
        return sLockCount > 0;
    }
}
