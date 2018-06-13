package lockmanager;

import static org.junit.jupiter.api.Assertions.*;

class LockTest {

    @org.junit.jupiter.api.Test
    void acquire() throws InterruptedException {
        Lock sLock = new Lock();
        sLock.acquire("S");
        assertEquals("S", sLock.getMode());

        Lock xLock = new Lock();
        xLock.acquire("X");
        assertEquals("X", xLock.getMode());

        Lock lockNoMode = new Lock();
        assertThrows(RuntimeException.class, () -> {
            lockNoMode.acquire(null);
        });
    }

    @org.junit.jupiter.api.Test
    void release() throws InterruptedException {
        Lock lock = new Lock();
        lock.release();
        assertNull(lock.getMode());

        lock.acquire("S");
        lock.release();
        assertNull(lock.getMode());

        lock.acquire("X");
        lock.release();
        assertNull(lock.getMode());
    }

    @org.junit.jupiter.api.Test
    void upgrade() throws InterruptedException {
        Lock lock = new Lock();
        lock.upgrade();
        assertEquals("X", lock.getMode());

        Lock lock2 = new Lock();
        lock2.acquire("S");
        lock2.upgrade();
        assertEquals("X", lock.getMode());

        Lock lock3 = new Lock();
        lock3.acquire("X");
        lock3.upgrade();
        assertEquals("X", lock.getMode());
    }

    @org.junit.jupiter.api.Test
    void getMode() throws InterruptedException {
        Lock sLock = new Lock();
        sLock.acquire("S");
        assertEquals("S", sLock.getMode());

        Lock xLock = new Lock();
        xLock.acquire("X");
        assertEquals("X", xLock.getMode());

        Lock newLock = new Lock();
        assertNull(newLock.getMode());
    }
}