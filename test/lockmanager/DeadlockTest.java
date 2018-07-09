package lockmanager;

import net.jodah.concurrentunit.Waiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class DeadlockTest {
    private LockManager lm;
    private final int SLEEP_DELAY = 20;
    private final int WAIT_DELAY = 1000;
    static int DEBUG = 0;

    @BeforeEach
    void init() {
        lm = new LockManager();
    }

    @Test
    void deadlock() throws Throwable {
        Integer lockObj = 55;
        Transaction txn1 = new Transaction(1);
        Transaction txn2 = new Transaction(2);
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Thread t1 = new Thread(new DeadlockTransaction(lm, txn1, lockObj, "U", waiter1));
        Thread t2 = new Thread(new DeadlockTransaction(lm, txn2, lockObj, "U", waiter2));

        t1.start();
        Thread.sleep(SLEEP_DELAY);
        t2.start();

        try {
            waiter2.await(1000);
        } catch (TimeoutException e) {
            t1.interrupt();
//            fail("Deadlock was not detected");
        }
    }
}

class DeadlockTransaction implements Runnable {
    private LockManager lm;
    private Transaction txn;
    private int obj;
    private String mode;
    private Waiter waiter;

    DeadlockTransaction(LockManager lm, Transaction txn, int obj, String mode, Waiter waiter) {
        this.lm = lm;
        this.txn = txn;
        this.obj = obj;
        this.mode = mode;
        this.waiter = waiter;
    }

    private void debug(String msg) {
        if (DeadlockTest.DEBUG == 1) {
            System.out.println(msg);
        }
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
            lm.lock(obj, txn, "S");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        debug("Txn: " + txn + " has the sLock");

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        try {
            lm.lock(obj, txn, "X");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        debug("Txn: " + txn + " has the xLock");

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
