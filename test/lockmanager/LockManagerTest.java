package lockmanager;

import net.jodah.concurrentunit.Waiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class LockManagerTest {
    private Integer txid;
    private Integer lockName;
    private LockManager lm;

    @BeforeEach
    void init() {
        txid = 1;
        lockName = 123;
        lm = new LockManager();
    }

    @Test
    void getSLock() throws InterruptedException {
        lm.lock(lockName, txid, "S");
        assertTrue(lm.hasLock(txid, lockName));
        assertEquals("S", lm.getLockMode(lockName));
    }

    @Test
    void getXLock() throws InterruptedException {
        lm.lock(lockName, txid, "X");
        assertTrue(lm.hasLock(txid, lockName));
        assertEquals("X", lm.getLockMode(lockName));
    }

    @Test
    void xLockBlocksSLock() throws Throwable {
        final Waiter waiter = new Waiter();
        lm.lock(lockName, txid, "X");

        new Thread(() -> {
            try {
                lm.lock(lockName, 2, "S");
            } catch (InterruptedException e) {}

            waiter.resume(); // should not get here
        }).start();

        assertThrows(TimeoutException.class, () -> {
            waiter.await(10);
        }, "Lock was acquired, but should have blocked");
    }

    @Test
    void xLockBlocksXLock() throws Throwable {
        final Waiter waiter = new Waiter();
        lm.lock(lockName, txid, "X");

        new Thread(() -> {
            try {
                lm.lock(lockName, 2, "X");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            waiter.resume(); // should not get here
        }).start();

        assertThrows(TimeoutException.class, () -> {
            waiter.await(10);
        }, "Lock was acquired, but should have been blocked");
    }

    @Test
    void sLockDoesNotBlockSLock() throws Throwable {
        final Waiter waiter = new Waiter();

        lm.lock(lockName, txid, "S");

        new Thread(() -> {
            try {
                lm.lock(lockName, 2, "S");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            waiter.resume();
        }).start();

        try {
            waiter.await(10);
        } catch (TimeoutException e) {
            fail("Lock was not acquired, but it should have been");
        }
    }

    @Test
    void upgradeBlockedBySLock() throws Throwable {
        final Waiter waiter = new Waiter();

        lm.lock(lockName, txid, "S");

        new Thread(() -> {
            try {
                lm.lock(lockName, 2, "S");
                lm.lock(lockName, 2, "X");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            waiter.resume(); // should not get here
        }).start();

        assertThrows(TimeoutException.class, () -> {
            waiter.await(10);
        }, "Lock was acquired, but should have been blocked");
    }

    @Test
    void upgradeBlockedByXLock() throws Throwable {
        final Waiter waiter = new Waiter();

        lm.lock(lockName, txid, "X");

        new Thread(() -> {
            try {
                lm.lock(lockName, 2, "S");
                lm.lock(lockName, 2, "X");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            waiter.resume(); // should not get here
        }).start();

        assertThrows(TimeoutException.class, () -> {
            waiter.await(10);
        }, "Lock was acquired, but should have been blocked");
    }

    @Test
    void removeTransactionWithSLock() throws Throwable {
        lm.lock(lockName, txid, "S");
        lm.removeTransaction(txid);
        assertFalse(lm.hasLock(txid, lockName));
    }

    @Test
    void removeTransactionWithXLock() throws Throwable {
        lm.lock(lockName, txid, "X");
        lm.removeTransaction(txid);
        assertFalse(lm.hasLock(txid, lockName));
    }

    @Test
    void removeXLockUnblocksSLock() throws Throwable {
        final Waiter waiter = new Waiter();

        lm.lock(lockName, txid, "X");

        new Thread(() -> {
            try {
                lm.lock(lockName, 2, "S");
            } catch(InterruptedException e) {}

            waiter.resume();
        }).start();

        assertThrows(TimeoutException.class, () -> {
            waiter.await(10);
        }, "Lock was acquired, but should have been blocked");

        lm.removeTransaction(txid);

        try {
            waiter.await(10);
        } catch (TimeoutException e) {
            fail("S lock was not acquired after X lock was released");
        }

        assertFalse(lm.hasLock(txid, lockName));
    }

    @Test
    void removeXLockUnblocksXLock() throws Throwable {
        final Waiter waiter = new Waiter();

        lm.lock(lockName, txid, "X");

        new Thread(() -> {
            try {
                lm.lock(lockName, 2, "X");
            } catch(InterruptedException e) {}

            waiter.resume();
        }).start();

        assertThrows(TimeoutException.class, () -> {
            waiter.await(10);
        }, "Lock was acquired, but should have been blocked");

        lm.removeTransaction(txid);

        try {
            waiter.await(10);
        } catch (TimeoutException e) {
            fail("Second X lock was not acquired after first X lock was released");
        }

        assertFalse(lm.hasLock(txid, lockName));
    }

    @Test
    void allLocksAreReleased() throws InterruptedException {
        Integer[] locks = {1, 2, 3, 4};
        for (int lockName: locks) {
            lm.lock(lockName, txid, "S");
        }
        lm.removeTransaction(txid);

        for (int lockName: locks) {
            assertFalse(lm.hasLock(txid, lockName));
        }
    }
}