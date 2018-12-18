package lockmanager;

import net.jodah.concurrentunit.Waiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    static int DEBUG = 0;
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
        Transaction txn1 = new Transaction(1);
        Transaction txn2 = new Transaction(2);
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txn1, lockObj, "X", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txn2, lockObj, "S", waiter2));

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
        Transaction txn1 = new Transaction(1);
        Transaction txn2 = new Transaction(2);
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txn1, lockObj, "S", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txn2, lockObj, "S", waiter2));

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
        Transaction txn1 = new Transaction(1);
        Transaction txn2 = new Transaction(2);
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txn1, lockObj, "S", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txn2, lockObj, "X", waiter2));

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
        Transaction txn1 = new Transaction(1);
        Transaction txn2 = new Transaction(2);
        Transaction txn3 = new Transaction(3);
        Transaction txn4 = new Transaction(4);
        Transaction txn5 = new Transaction(5);
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Waiter waiter3 = new Waiter();
        Waiter waiter4 = new Waiter();
        Waiter waiter5 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txn1, lockObj, "X", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txn2, lockObj, "S", waiter2));
        Thread t3 = new Thread(new TransactionTest(lm, txn3, lockObj, "S", waiter3));
        Thread t4 = new Thread(new TransactionTest(lm, txn4, lockObj, "X", waiter4));
        Thread t5 = new Thread(new TransactionTest(lm, txn5, lockObj, "S", waiter5));

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
        assertTrue(lm.hasLock(txn2, lockObj));
        assertTrue(lm.hasLock(txn3, lockObj));
        assertFalse(lm.hasLock(txn5, lockObj));
        assertFalse(lm.hasLock(txn4, lockObj));
        waiter4.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txn4, lockObj));
        waiter5.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txn5, lockObj));
    }

    @Test
    void upgradeHappensBeforeXLock() throws Throwable {
        Integer lockObj = 55;
        Transaction txn1 = new Transaction(1);
        Transaction txn2 = new Transaction(2);
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txn1, lockObj, "U", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txn2, lockObj, "X", waiter2));

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
    void blockedSLockAddsToWaitForGraph() throws Throwable {
        Integer lockObj = 55;
        Transaction txn1 = new Transaction(1);
        Transaction txn2 = new Transaction(2);
        WaitForGraph graph = lm.getWaitForGraph();
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(() -> {
            try {
                lm.lock(lockObj, txn1, Lock.LockMode.EXCLUSIVE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            waiter1.resume();

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            lm.removeTransaction(txn1);

        });
        Thread t2 = new Thread(() -> {
            try {
                lm.lock(lockObj, txn2, Lock.LockMode.SHARED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            waiter2.resume();

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            lm.removeTransaction(txn2);
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
        Integer lockObj = 55;
        Transaction txn1 = new Transaction(1);
        Transaction txn2 = new Transaction(2);
        WaitForGraph graph = lm.getWaitForGraph();
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(() -> {
            try {
                lm.lock(lockObj, txn1, Lock.LockMode.EXCLUSIVE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            waiter1.resume();

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            lm.removeTransaction(txn1);

        });
        Thread t2 = new Thread(() -> {
            try {
                lm.lock(lockObj, txn2, Lock.LockMode.EXCLUSIVE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            waiter2.resume();

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            lm.removeTransaction(txn2);
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
        Integer lockObj = 55;
        Transaction txn1 = new Transaction(1);
        Transaction txn2 = new Transaction(2);
        Transaction txn3 = new Transaction(3);
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Waiter waiter3 = new Waiter();
        Thread t1 = new Thread(new TransactionTest(lm, txn1, lockObj, "S", waiter1));
        Thread t2 = new Thread(new TransactionTest(lm, txn2, lockObj, "X", waiter2));
        Thread t3 = new Thread(new TransactionTest(lm, txn3, lockObj, "S", waiter3));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();
        Thread.sleep(SLEEP_DELAY);
        t3.start();

        waiter1.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txn1, lockObj));
        assertFalse(lm.hasLock(txn2, lockObj));
        assertFalse(lm.hasLock(txn3, lockObj));
        waiter2.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txn2, lockObj));
        assertFalse(lm.hasLock(txn3, lockObj));
        waiter3.await(WAIT_DELAY);
        assertTrue(lm.hasLock(txn3, lockObj));
    }
}

class TransactionTest implements Runnable {
    private LockManager lm;
    private Transaction txn;
    private int obj;
    private String mode;
    private Waiter waiter;

    TransactionTest(LockManager lm, Transaction txn, int obj, String mode, Waiter waiter) {
        this.lm = lm;
        this.txn = txn;
        this.obj = obj;
        this.mode = mode;
        this.waiter = waiter;
    }

    private void debug(String msg) {
        if (IntegrationTest.DEBUG == 1)
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
            lm.lock(obj, txn, mode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        debug("Txn: " + txn + " has the lock");
        waiter.resume();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        debug("Txn: " + txn + " released the lock");
        lm.removeTransaction(txn);
    }

    private void upgrade() {
        try {
            lm.lock(obj, txn, Lock.LockMode.SHARED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        debug("Txn: " + txn + " has the Slock");

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        try {
            lm.lock(obj, txn, Lock.LockMode.EXCLUSIVE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        debug("Txn: " + txn + " has the Xlock");

        waiter.resume();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        debug("Txn: " + txn + " released the lock");
        lm.removeTransaction(txn);
    }
}
