package lockmanager;

import net.jodah.concurrentunit.Waiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    private LockManager lm;
    private final int SLEEP_DELAY = 20;
    private final int WAIT_DELAY = 1000;

    @BeforeEach
    void init() {
        lm = new LockManager();
    }

    @Test
    void sLockWaitsOnXLock() throws Throwable {
        Integer lockObj = 55;
        Integer txn1 = 1;
        Integer txn2 = 2;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new Transaction(lm, txn1, lockObj, "X", waiter1));
        Thread t2 = new Thread(new Transaction(lm, txn2, lockObj, "S", waiter2));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();

        waiter1.await(WAIT_DELAY);
        assertFalse(lm.hasLock(txn2, lockObj));
        assertTrue(lm.hasLock(txn1, lockObj));
        waiter2.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txn2, lockObj));
    }

    @Test
    void sLockNoWaitOnSLock() throws Throwable {
        Integer lockObj = 55;
        Integer txn1 = 1;
        Integer txn2 = 2;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new Transaction(lm, txn1, lockObj, "S", waiter1));
        Thread t2 = new Thread(new Transaction(lm, txn2, lockObj, "S", waiter2));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();

        waiter1.await(WAIT_DELAY);
        waiter2.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txn1, lockObj));
        assertTrue(lm.hasLock(txn2, lockObj));
    }

    @Test
    void xLockWaitsOnSLock() throws Throwable {
        Integer lockObj = 55;
        Integer txn1 = 1;
        Integer txn2 = 2;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new Transaction(lm, txn1, lockObj, "S", waiter1));
        Thread t2 = new Thread(new Transaction(lm, txn2, lockObj, "X", waiter2));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();

        waiter1.await(WAIT_DELAY);
        assertFalse(lm.hasLock(txn2, lockObj));
        assertTrue(lm.hasLock(txn1, lockObj));
        waiter2.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txn2, lockObj));
    }

    @Test
    void lockSequence() throws Throwable {
        Integer lockObj = 55;
        Integer txn1 = 1;
        Integer txn2 = 2;
        Integer txn3 = 3;
        Integer txn4 = 4;
        Integer txn5 = 5;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Waiter waiter3 = new Waiter();
        Waiter waiter4 = new Waiter();
        Waiter waiter5 = new Waiter();
        Thread t1 = new Thread(new Transaction(lm, txn1, lockObj, "X", waiter1));
        Thread t2 = new Thread(new Transaction(lm, txn2, lockObj, "S", waiter2));
        Thread t3 = new Thread(new Transaction(lm, txn3, lockObj, "S", waiter3));
        Thread t4 = new Thread(new Transaction(lm, txn4, lockObj, "X", waiter4));
        Thread t5 = new Thread(new Transaction(lm, txn5, lockObj, "S", waiter5));

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
        assertTrue(lm.hasLock(txn1, lockObj));
        assertFalse(lm.hasLock(txn2, lockObj));
        assertFalse(lm.hasLock(txn3, lockObj));
        assertFalse(lm.hasLock(txn4, lockObj));
        assertFalse(lm.hasLock(txn5, lockObj));
        waiter2.await(WAIT_DELAY);
        waiter3.await(WAIT_DELAY);
        waiter5.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txn2, lockObj));
        assertTrue(lm.hasLock(txn3, lockObj));
        assertTrue(lm.hasLock(txn5, lockObj));
        assertFalse(lm.hasLock(txn4, lockObj));
        waiter4.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txn4, lockObj));
    }

    @Test
    void upgradeHappensBeforeXLock() throws Throwable {
        Integer lockObj = 55;
        Integer txn1 = 1;
        Integer txn2 = 2;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new Transaction(lm, txn1, lockObj, "U", waiter1));
        Thread t2 = new Thread(new Transaction(lm, txn2, lockObj, "X", waiter2));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();

        waiter1.await(WAIT_DELAY);
        assertFalse(lm.hasLock(txn2, lockObj));
        assertTrue(lm.hasLock(txn1, lockObj));
        waiter2.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txn2, lockObj));
    }
}

class Transaction implements Runnable {
    private LockManager lm;
    private int txn;
    private int obj;
    private String mode;
    private Waiter waiter;

    Transaction(LockManager lm, int txn, int obj, String mode, Waiter waiter) {
        this.lm = lm;
        this.txn = txn;
        this.obj = obj;
        this.mode = mode;
        this.waiter = waiter;
    }

    public void run() {
        if (mode.equals("U")) {
            upgrade();
        } else {
            lock();
        }
    }

    private void lock() {
        try {
            lm.lock(obj, txn, mode);
        } catch (InterruptedException e) {}
        System.out.println("Txn: " + txn + " has the lock");
        waiter.resume();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Txn: " + txn + " released the lock");
        lm.removeTransaction(txn);
    }

    private void upgrade() {
        try {
            lm.lock(obj, txn, "S");
        } catch (InterruptedException e) {}

        System.out.println("Txn: " + txn + " has the Slock");

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        try {
            lm.lock(obj, txn, "X");
        } catch (InterruptedException e) {}

        System.out.println("Txn: " + txn + " has the Xlock");

        waiter.resume();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Txn: " + txn + " released the lock");
        lm.removeTransaction(txn);
    }
}
