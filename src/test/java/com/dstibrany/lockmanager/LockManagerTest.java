package com.dstibrany.lockmanager;

import net.jodah.concurrentunit.Waiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class LockManagerTest {
    private int txn;
    private int lockName;
    private LockManager lm;

    @BeforeEach
    void init() {
        txn = 1;
        lockName = 123;
        lm = new LockManager();
    }

    @Test
    void getSLock() throws DeadlockException {
        lm.lock(lockName, txn, Lock.LockMode.SHARED);
        assertTrue(lm.hasLock(txn, lockName));
        assertEquals(Lock.LockMode.SHARED, lm.getLockMode(lockName));
    }

    @Test
    void getXLock() throws DeadlockException {
        lm.lock(lockName, txn, Lock.LockMode.EXCLUSIVE);
        assertTrue(lm.hasLock(txn, lockName));
        assertEquals(Lock.LockMode.EXCLUSIVE, lm.getLockMode(lockName));
    }

    @Test
    void xLockBlocksSLock() throws Throwable {
        final Waiter waiter = new Waiter();
        lm.lock(lockName, txn, Lock.LockMode.EXCLUSIVE);

        new Thread(() -> {
            try {
                lm.lock(lockName, 2, Lock.LockMode.SHARED);
            } catch (DeadlockException e) {
            }

            waiter.resume(); // should not get here
        }).start();

        assertThrows(TimeoutException.class, () -> {
            waiter.await(10);
        }, "Lock was acquired, but should have blocked");
    }

    @Test
    void xLockBlocksXLock() throws Throwable {
        final Waiter waiter = new Waiter();
        lm.lock(lockName, txn, Lock.LockMode.EXCLUSIVE);

        new Thread(() -> {
            try {
                lm.lock(lockName, 2, Lock.LockMode.EXCLUSIVE);
            } catch (DeadlockException e) {
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

        lm.lock(lockName, txn, Lock.LockMode.SHARED);

        new Thread(() -> {
            try {
                lm.lock(lockName, 2, Lock.LockMode.SHARED);
            } catch (DeadlockException e) {
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

        lm.lock(lockName, txn, Lock.LockMode.SHARED);

        new Thread(() -> {
            int txnId = 2;
            try {
                lm.lock(lockName, txnId, Lock.LockMode.SHARED);
                lm.lock(lockName, txnId, Lock.LockMode.EXCLUSIVE);
            } catch (DeadlockException e) {
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

        lm.lock(lockName, txn, Lock.LockMode.EXCLUSIVE);

        new Thread(() -> {
            int txnId = 2;
            try {
                lm.lock(lockName, txnId, Lock.LockMode.SHARED);
                lm.lock(lockName, txnId, Lock.LockMode.EXCLUSIVE);
            } catch (DeadlockException e) {
            }
            waiter.resume(); // should not get here
        }).start();

        assertThrows(TimeoutException.class, () -> {
            waiter.await(10);
        }, "Lock was acquired, but should have been blocked");
    }

    @Test
    void removeTransactionWithSLock() throws Throwable {
        lm.lock(lockName, txn, Lock.LockMode.SHARED);
        lm.removeTransaction(txn);
        assertFalse(lm.hasLock(txn, lockName));
    }

    @Test
    void removeTransactionWithXLock() throws Throwable {
        lm.lock(lockName, txn, Lock.LockMode.EXCLUSIVE);
        lm.removeTransaction(txn);
        assertFalse(lm.hasLock(txn, lockName));
    }

    @Test
    void removeXLockUnblocksSLock() throws Throwable {
        final Waiter waiter = new Waiter();

        lm.lock(lockName, txn, Lock.LockMode.EXCLUSIVE);

        new Thread(() -> {
            int txnId = 2;
            try {
                lm.lock(lockName, txnId, Lock.LockMode.SHARED);
            } catch (DeadlockException e) {
            }

            waiter.resume();
        }).start();

        assertThrows(TimeoutException.class, () -> {
            waiter.await(10);
        }, "Lock was acquired, but should have been blocked");

        lm.removeTransaction(txn);

        try {
            waiter.await(10);
        } catch (TimeoutException e) {
            fail("S lock was not acquired after X lock was released");
        }

        assertFalse(lm.hasLock(txn, lockName));
    }

    @Test
    void removeXLockUnblocksXLock() throws Throwable {
        final Waiter waiter = new Waiter();

        lm.lock(lockName, txn, Lock.LockMode.EXCLUSIVE);

        new Thread(() -> {
            int txnId = 2;
            try {
                lm.lock(lockName, txnId, Lock.LockMode.EXCLUSIVE);
            } catch (DeadlockException e) {
            }

            waiter.resume();
        }).start();

        assertThrows(TimeoutException.class, () -> {
            waiter.await(10);
        }, "Lock was acquired, but should have been blocked");

        lm.removeTransaction(txn);

        try {
            waiter.await(10);
        } catch (TimeoutException e) {
            fail("Second X lock was not acquired after first X lock was released");
        }

        assertFalse(lm.hasLock(txn, lockName));
    }

    @Test
    void allLocksAreReleased() throws DeadlockException {
        Integer[] locks = {1, 2, 3, 4};
        for (int lockName : locks) {
            lm.lock(lockName, txn, Lock.LockMode.SHARED);
        }
        lm.removeTransaction(txn);

        for (int lockName : locks) {
            assertFalse(lm.hasLock(txn, lockName));
        }
    }
}