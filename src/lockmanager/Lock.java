package lockmanager;

class Lock {
    private int xLockCount = 0;
    private int sLockCount = 0;
    private Thread xOwner;

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

    synchronized void release() {
        if (sLockCount > 0) {
            sLockCount--;
        }
        if (xLockCount == 1) {
            xLockCount = 0;
            xOwner = null;
        }

        this.notifyAll();
    }

    synchronized void upgrade() throws InterruptedException {
        if (xOwner == Thread.currentThread()) return;

        while (isXLocked() || sLockCount > 1) {
            this.wait();
        }
        sLockCount = 0;
        xLockCount = 1;
    }

    synchronized String getMode() {
        if (isXLocked()) return "X";
        else if (isSLocked()) return "S";
        else return null;
    }

    // TODO: prevent barging
    private synchronized void acquireSLock() throws InterruptedException {
        while (isXLocked()) {
           this.wait();
        }
        sLockCount++;
    }

    private synchronized void acquireXLock() throws InterruptedException {
        while (isXLocked() || isSLocked()) {
            this.wait();
        }
        xLockCount = 1;
        xOwner = Thread.currentThread();
    }

    private boolean isXLocked() {
        return xLockCount == 1;
    }

    private boolean isSLocked() {
        return sLockCount > 0;
    }
}
