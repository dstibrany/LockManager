package lockmanager;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.LockSupport;

class Lock {
    private ConcurrentLinkedDeque<LockTuple> waitQueue = new ConcurrentLinkedDeque<>();
    private Boolean locked = false;
    private Integer readCount = 0;
    private String mode;

    void acquire(String lockRequestMode) {
        Thread current = Thread.currentThread();

        waitQueue.add(new LockTuple(current, lockRequestMode));
        if (lockRequestMode.equals("S")) {
            acquireSLock();
            mode = "S";
        } else {
            acquireXLock();
            mode = "X";
        }
        waitQueue.remove();
    }

    synchronized void release() {
        if (mode.equals("X")) {
            locked = false;
            if (waitQueue.peek().getRequestMode().equals("X")) {
                LockSupport.unpark(waitQueue.peek().getThread());
            } else {
                Iterator<LockTuple> i = waitQueue.iterator();
                while (i.hasNext()) {
                    LockTuple next = i.next();
                    if (!next.getRequestMode().equals("S")) break;
                    synchronized (this) {
                        LockSupport.unpark(next.getThread());
                    }
                }
            }
        } else if (mode.equals("S")) {
            readCount--;
            if (waitQueue.peek() != null) {
                LockSupport.unpark(waitQueue.peek().getThread());
            }
        }
    }

    private void acquireSLock() {
        while (!s()) {
            LockSupport.park();
        }
    }

    private void acquireXLock() {
        while (!x()) {
            LockSupport.park();
        }
    }

    private synchronized boolean s() {
        Thread current = Thread.currentThread();
        if (!locked && waitQueue.peek().getThread() == current) {
            readCount++;
            return true;
        }
        return false;
    }

    private synchronized boolean x() {
        Thread current = Thread.currentThread();
        if (!locked && readCount == 0 && waitQueue.peek().getThread() == current) {
            locked = true;
            return true;
        }
        return false;
    }

}

class LockTuple {
    private Thread thread;
    private String requestMode;

    LockTuple(Thread thread, String requestMode) {
        this.thread = thread;
        this.requestMode = requestMode;
    }

    Thread getThread() {
        return thread;
    }

    String getRequestMode() {
        return requestMode;
    }
}

