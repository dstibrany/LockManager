package lockmanager;

import net.jodah.concurrentunit.Waiter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class DeadlockTest {

    // TODO: write more DL tests
    @Test
    void deadlockDetectedOnTwoTransactions() throws Exception {
        LockManager lm = new LockManager();
        int lockA = 99;
        int lockB = 100;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();

        Thread t1 = new Thread(() -> {
            Transaction txn1 = new Transaction(1);
            try {
                lm.lock(lockA, txn1, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(100);
                lm.lock(lockB, txn1, Lock.LockMode.EXCLUSIVE);
                waiter1.resume();
            } catch (DeadlockException e) {
                waiter1.fail("Txn1 should not have been aborted by deadlock detector");
            } catch (InterruptedException e) {}
        });

        Thread t2 = new Thread(() -> {
            Transaction txn2 = new Transaction(2);
            try {
                lm.lock(lockB, txn2, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(100);
                lm.lock(lockA, txn2, Lock.LockMode.EXCLUSIVE);
                waiter2.fail("Txn2 should have been aborted by the deadlock detector");
            } catch (DeadlockException e) {
                waiter2.resume();
            } catch (InterruptedException e) {}
        });

        t1.start();
        t2.start();

        try {
            waiter1.await(2000);
            waiter2.await(2000);
        } catch (TimeoutException e) {
            fail("Deadlock was not resolved");
        }
    }
}
