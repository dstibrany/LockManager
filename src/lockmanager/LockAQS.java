package lockmanager;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

class LockAQS {
    private static class Sync extends AbstractQueuedSynchronizer {
        protected boolean tryAcquire(int acquires) {
            return compareAndSetState(0, 1);
        }
        protected boolean tryRelease(int releases) {
            setState(0);
            return true;
        }
        protected int tryAcquireShared(int acquires) {
            int state = getState();
            if (compareAndSetState(state, ++state)) {
                return 1;
            } else {
                return -1;
            }
        }
        protected boolean tryReleaseShared(int releases) {
            int state = getState();
            if (compareAndSetState(state, ++state)) {
                return 1;
            } else {
                return -1;
            }
            return true;
        }
    }

    private final Sync sync = new Sync();

    void lock() {
       sync.acquire(1);
    }

    void unlock() {
        sync.release(1);
    }
}