package com.dstibrany.lockmanager;

import net.jodah.concurrentunit.Waiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemTest {
    static int DEBUG = 0;
    private final int SLEEP_DELAY = 20;
    private final int WAIT_DELAY = 1000;
    private LockManager lm;

    @BeforeEach
    void init() {
        lm = new LockManager();
    }

    @Test
    void sLockWaitsOnXLock() throws Throwable {
        int lockObj = 55;
        int txnId1 = 1;
        int txnId2 = 2;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txnId1, lockObj, "X", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txnId2, lockObj, "S", waiter2));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();

        waiter1.await(WAIT_DELAY);
        assertFalse(lm.hasLock(txnId2, lockObj));
        assertTrue(lm.hasLock(txnId1, lockObj));
        waiter2.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txnId2, lockObj));
    }

    @Test
    void sLockNoWaitOnSLock() throws Throwable {
        int lockObj = 55;
        int txnId1 = 1;
        int txnId2 = 2;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txnId1, lockObj, "S", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txnId2, lockObj, "S", waiter2));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();

        waiter1.await(WAIT_DELAY);
        waiter2.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txnId1, lockObj));
        assertTrue(lm.hasLock(txnId2, lockObj));
    }

    @Test
    void xLockWaitsOnSLock() throws Throwable {
        int lockObj = 55;
        int txnId1 = 1;
        int txnId2 = 2;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txnId1, lockObj, "S", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txnId2, lockObj, "X", waiter2));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();

        waiter1.await(WAIT_DELAY);
        assertFalse(lm.hasLock(txnId2, lockObj));
        assertTrue(lm.hasLock(txnId1, lockObj));
        waiter2.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txnId2, lockObj));
    }

    @Test
    void lockSequence() throws Throwable {
        int lockObj = 55;
        int txnId1 = 1;
        int txnId2 = 2;
        int txnId3 = 3;
        int txnId4 = 4;
        int txnId5 = 5;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Waiter waiter3 = new Waiter();
        Waiter waiter4 = new Waiter();
        Waiter waiter5 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txnId1, lockObj, "X", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txnId2, lockObj, "S", waiter2));
        Thread t3 = new Thread(new TransactionTest(lm, txnId3, lockObj, "S", waiter3));
        Thread t4 = new Thread(new TransactionTest(lm, txnId4, lockObj, "X", waiter4));
        Thread t5 = new Thread(new TransactionTest(lm, txnId5, lockObj, "S", waiter5));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();
        Thread.sleep(SLEEP_DELAY);
        t3.start();
        Thread.sleep(SLEEP_DELAY);
        t4.start();
        Thread.sleep(SLEEP_DELAY);
        t5.start();

        waiter1.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txnId1, lockObj));
        assertFalse(lm.hasLock(txnId2, lockObj));
        assertFalse(lm.hasLock(txnId3, lockObj));
        assertFalse(lm.hasLock(txnId4, lockObj));
        assertFalse(lm.hasLock(txnId5, lockObj));
        waiter2.await(WAIT_DELAY);
        waiter3.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txnId2, lockObj));
        assertTrue(lm.hasLock(txnId3, lockObj));
        assertFalse(lm.hasLock(txnId5, lockObj));
        assertFalse(lm.hasLock(txnId4, lockObj));
        waiter4.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txnId4, lockObj));
        waiter5.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txnId5, lockObj));
    }

    @Test
    void upgradeHappensBeforeXLock() throws Throwable {
        int lockObj = 55;
        int txnId1 = 1;
        int txnId2 = 2;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txnId1, lockObj, "U", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txnId2, lockObj, "X", waiter2));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();

        waiter1.await(WAIT_DELAY);
        assertFalse(lm.hasLock(txnId2, lockObj));
        assertTrue(lm.hasLock(txnId1, lockObj));
        waiter2.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txnId2, lockObj));
    }

    @Test
    void blockedSLockAddsToWaitForGraph() throws Throwable {
        int lockObj = 55;
        int txnId1 = 1;
        int txnId2 = 2;
        Transaction txn1 = new Transaction(txnId1);
        Transaction txn2 = new Transaction(txnId2);
        WaitForGraph graph = lm.getWaitForGraph();
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(() -> {
            try {
                lm.lock(lockObj, txnId1, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(200);
                waiter1.resume();
                Thread.sleep(100);
            } catch (Exception e) {

            }

            lm.removeTransaction(txnId1);
        });
        Thread t2 = new Thread(() -> {
            try {
                lm.lock(lockObj, txnId2, Lock.LockMode.SHARED);
                waiter2.resume();
                Thread.sleep(100);
            } catch (Exception e) {

            }

            lm.removeTransaction(txnId2);
        });

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();
        waiter1.await(WAIT_DELAY);
        assertTrue(graph.hasEdge(txn2, txn1));
        assertFalse(graph.hasEdge(txn1, txn2));
        waiter2.await(WAIT_DELAY);
        assertFalse(graph.hasEdge(txn2, txn1));
        assertFalse(graph.hasEdge(txn1, txn2));
    }

    @Test
    void blockedXLockAddsToWaitForGraph() throws Throwable {
        int lockObj = 55;
        int txnId1 = 1;
        int txnId2 = 2;
        Transaction txn1 = new Transaction(txnId1);
        Transaction txn2 = new Transaction(txnId2);
        WaitForGraph graph = lm.getWaitForGraph();
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(() -> {
            try {
                lm.lock(lockObj, txnId1, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(200);
                waiter1.resume();
                Thread.sleep(100);
            } catch (Exception e) {

            }

            lm.removeTransaction(txnId1);

        });
        Thread t2 = new Thread(() -> {
            try {
                lm.lock(lockObj, txnId2, Lock.LockMode.EXCLUSIVE);
                waiter2.resume();
                Thread.sleep(100);
            } catch (Exception e) {
            }

            lm.removeTransaction(txnId2);
        });

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();
        waiter1.await(WAIT_DELAY);
        assertTrue(graph.hasEdge(txn2, txn1));
        assertFalse(graph.hasEdge(txn1, txn2));
        waiter2.await(WAIT_DELAY);
        assertFalse(graph.hasEdge(txn2, txn1));
        assertFalse(graph.hasEdge(txn1, txn2));
    }

    @Test
    void preventBarging() throws Throwable {
        int lockObj = 55;
        int txnId1 = 1;
        int txnId2 = 2;
        int txnId3 = 3;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Waiter waiter3 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txnId1, lockObj, "S", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txnId2, lockObj, "X", waiter2));
        Thread t3 = new Thread(new TransactionTest(lm, txnId3, lockObj, "S", waiter3));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();
        Thread.sleep(SLEEP_DELAY);
        t3.start();

        waiter1.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txnId1, lockObj));
        assertFalse(lm.hasLock(txnId2, lockObj));
        assertFalse(lm.hasLock(txnId3, lockObj));
        waiter2.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txnId2, lockObj));
        assertFalse(lm.hasLock(txnId3, lockObj));
        waiter3.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txnId3, lockObj));
    }
}

class TransactionTest implements Runnable {
    private LockManager lm;
    private int txnId;
    private int obj;
    private String mode;
    private Waiter waiter;

    TransactionTest(LockManager lm, int txnId, int obj, String mode, Waiter waiter) {
        this.lm = lm;
        this.txnId = txnId;
        this.obj = obj;
        this.mode = mode;
        this.waiter = waiter;
    }

    private void debug(String msg) {
        if (SystemTest.DEBUG == 1)
            System.out.println(msg);
    }

    public void run() {
        if (mode.equals("U")) {
            upgrade();
        } else {
            lock(mode.equals("S") ? Lock.LockMode.SHARED : Lock.LockMode.EXCLUSIVE);
        }
    }

    private void lock(Lock.LockMode mode) {
        try {
            lm.lock(obj, txnId, mode);
            debug("Txn: " + txnId + " has the lock");
            waiter.resume();
            Thread.sleep(100);
        } catch (Exception e) {

        }

        debug("Txn: " + txnId + " released the lock");
        lm.removeTransaction(txnId);
    }

    private void upgrade() {
        try {
            lm.lock(obj, txnId, Lock.LockMode.SHARED);
            debug("Txn: " + txnId + " has the Slock");
            Thread.sleep(100);
            lm.lock(obj, txnId, Lock.LockMode.EXCLUSIVE);
            debug("Txn: " + txnId + " has the Xlock");
            waiter.resume();
            Thread.sleep(100);
        } catch (Exception e) {

        }

        debug("Txn: " + txnId + " released the lock");
        lm.removeTransaction(txnId);
    }
}
