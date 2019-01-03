package lockmanager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LockTest {
    private Transaction txn1;
    private WaitForGraph waitForGraph;

    @BeforeEach
    void init() {
        txn1 = new Transaction(1);
        waitForGraph = new WaitForGraph();
    }

    @Test
    void acquire() throws InterruptedException {
        Lock sLock = new Lock(waitForGraph);
        sLock.acquire(txn1, Lock.LockMode.SHARED);
        assertEquals(Lock.LockMode.SHARED, sLock.getMode());

        Lock xLock = new Lock(waitForGraph);
        xLock.acquire(txn1, Lock.LockMode.EXCLUSIVE);
        assertEquals(Lock.LockMode.EXCLUSIVE, xLock.getMode());

        Lock lockNoMode = new Lock(waitForGraph);
        assertThrows(RuntimeException.class, () -> {
            lockNoMode.acquire(txn1, null);
        });
    }

    @Test
    void release() throws InterruptedException {
        Lock lock = new Lock(waitForGraph);
        lock.release(txn1);
        assertNull(lock.getMode());

        lock.acquire(txn1, Lock.LockMode.SHARED);
        lock.release(txn1);
        assertNull(lock.getMode());

        lock.acquire(txn1, Lock.LockMode.EXCLUSIVE);
        lock.release(txn1);
        assertNull(lock.getMode());
    }

    @Test
    void upgrade() throws InterruptedException {
        Lock lock = new Lock(waitForGraph);
        lock.upgrade(txn1);
        assertEquals(Lock.LockMode.EXCLUSIVE, lock.getMode());

        Lock lock2 = new Lock(waitForGraph);
        lock2.acquire(txn1, Lock.LockMode.SHARED);
        lock2.upgrade(txn1);
        assertEquals(Lock.LockMode.EXCLUSIVE, lock.getMode());

        Lock lock3 = new Lock(waitForGraph);
        lock3.acquire(txn1, Lock.LockMode.EXCLUSIVE);
        lock3.upgrade(txn1);
        assertEquals(Lock.LockMode.EXCLUSIVE, lock.getMode());
    }

    @Test
    void getMode() throws InterruptedException {
        Lock sLock = new Lock(waitForGraph);
        sLock.acquire(txn1, Lock.LockMode.SHARED);
        assertEquals(Lock.LockMode.SHARED, sLock.getMode());

        Lock xLock = new Lock(waitForGraph);
        xLock.acquire(txn1, Lock.LockMode.EXCLUSIVE);
        assertEquals(Lock.LockMode.EXCLUSIVE, xLock.getMode());

        Lock newLock = new Lock(waitForGraph);
        assertNull(newLock.getMode());
    }

    @Test
    void getOwnersSLock() throws InterruptedException {
        Lock sLock = new Lock(waitForGraph);
        sLock.acquire(txn1, Lock.LockMode.SHARED);
        assertEquals(1, sLock.getOwners().size());
        sLock.release(txn1);
        assertEquals(0, sLock.getOwners().size());
    }

    @Test
    void getOwnersXLock() throws InterruptedException {
        Lock xLock = new Lock(waitForGraph);
        xLock.acquire(txn1, Lock.LockMode.EXCLUSIVE);
        assertEquals(1, xLock.getOwners().size());
        xLock.release(txn1);
        assertEquals(0, xLock.getOwners().size());
    }


    @Test
    void getOwnersMultipleSLocks() throws InterruptedException {
        Transaction t1 = new Transaction(1);
        Transaction t2 = new Transaction(2);
        Transaction t3 = new Transaction(3);

        Lock sLock = new Lock(waitForGraph);

        sLock.acquire(t1, Lock.LockMode.SHARED);
        sLock.acquire(t2, Lock.LockMode.SHARED);
        sLock.acquire(t3, Lock.LockMode.SHARED);

        assertEquals(3, sLock.getOwners().size());

        sLock.release(t1);

        assertEquals(2, sLock.getOwners().size());
        assertFalse(sLock.getOwners().contains(t1));
        assertTrue(sLock.getOwners().contains(t2));
        assertTrue(sLock.getOwners().contains(t3));

        sLock.release(t2);
        assertEquals(1, sLock.getOwners().size());
        assertFalse(sLock.getOwners().contains(t1));
        assertFalse(sLock.getOwners().contains(t2));
        assertTrue(sLock.getOwners().contains(t3));

        sLock.release(t3);
        assertEquals(0, sLock.getOwners().size());
        assertFalse(sLock.getOwners().contains(t1));
        assertFalse(sLock.getOwners().contains(t2));
        assertFalse(sLock.getOwners().contains(t3));
    }
}