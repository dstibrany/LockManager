package lockmanager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LockTest {
    private Transaction txn1;
    
    @BeforeEach
    void init() {
        txn1 = new Transaction(1);
    }

    @Test
    void acquire() throws InterruptedException {
        Lock sLock = new Lock();
        sLock.acquire(txn1, "S");
        assertEquals("S", sLock.getMode());

        Lock xLock = new Lock();
        xLock.acquire(txn1, "X");
        assertEquals("X", xLock.getMode());

        Lock lockNoMode = new Lock();
        assertThrows(RuntimeException.class, () -> {
            lockNoMode.acquire(txn1, null);
        });
    }

    @Test
    void release() throws InterruptedException {
        Lock lock = new Lock();
        lock.release(txn1);
        assertNull(lock.getMode());

        lock.acquire(txn1,"S");
        lock.release(txn1);
        assertNull(lock.getMode());

        lock.acquire(txn1,"X");
        lock.release(txn1);
        assertNull(lock.getMode());
    }

    @Test
    void upgrade() throws InterruptedException {
        Lock lock = new Lock();
        lock.upgrade(txn1);
        assertEquals("X", lock.getMode());

        Lock lock2 = new Lock();
        lock2.acquire(txn1,"S");
        lock2.upgrade(txn1);
        assertEquals("X", lock.getMode());

        Lock lock3 = new Lock();
        lock3.acquire(txn1, "X");
        lock3.upgrade(txn1);
        assertEquals("X", lock.getMode());
    }

    @Test
    void getMode() throws InterruptedException {
        Lock sLock = new Lock();
        sLock.acquire(txn1,"S");
        assertEquals("S", sLock.getMode());

        Lock xLock = new Lock();
        xLock.acquire(txn1, "X");
        assertEquals("X", xLock.getMode());

        Lock newLock = new Lock();
        assertNull(newLock.getMode());
    }

    @Test
    void getOwnersSLock() throws InterruptedException {
        Lock sLock = new Lock();
        sLock.acquire(txn1,"S");
        assertEquals(1, sLock.getOwners().size());
        sLock.release(txn1);
        assertEquals(0, sLock.getOwners().size());
    }
    
    @Test
    void getOwnersXLock() throws InterruptedException {
        Lock xLock = new Lock();
        xLock.acquire(txn1,"X");
        assertEquals(1, xLock.getOwners().size());
        xLock.release(txn1);
        assertEquals(0, xLock.getOwners().size());
    }


    @Test
    void getOwnersMultipleSLocks() throws InterruptedException {
        Transaction t1 = new Transaction(1);
        Transaction t2 = new Transaction(2);
        Transaction t3 = new Transaction(3);

        Lock sLock = new Lock();

        sLock.acquire(t1, "S");
        sLock.acquire(t2, "S");
        sLock.acquire(t3, "S");

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