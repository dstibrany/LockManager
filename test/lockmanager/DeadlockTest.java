package lockmanager;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeadlockTest {
    // TODO: write more DL tests
    @Test
    void deadlockTwoTransactions() throws Exception {
        LockManager lm = new LockManager();
        int lockA = 99;
        int lockB = 100;

        Thread t1 = new Thread(() -> {
            Transaction txn1 = new Transaction(1);
            try {
                lm.lock(lockA, txn1, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(100);
                lm.lock(lockB, txn1, Lock.LockMode.EXCLUSIVE);
                System.out.printf("Thread: %d Txn1 IN CRITICAL SECTION\n", Thread.currentThread().getId());
                lm.removeTransaction(txn1);
            } catch (DeadlockException e) {
                System.out.printf("Thread: %d Txn1 Deadlock detected\n", Thread.currentThread().getId());
            } catch (InterruptedException e) {}
        });

        Thread t2 = new Thread(() -> {
            Transaction txn2 = new Transaction(2);
            try {
                lm.lock(lockB, txn2, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(100);
                lm.lock(lockA, txn2, Lock.LockMode.EXCLUSIVE);
                System.out.printf("Thread: %d Txn2 IN CRITICAL SECTION\n", Thread.currentThread().getId());
                lm.removeTransaction(txn2);
            } catch (DeadlockException e) {
                System.out.printf("Thread: %d Txn2 Deadlock detected\n", Thread.currentThread().getId());
            } catch (InterruptedException e) {}
        });
        Thread t3 = new Thread(() -> {
            Transaction txn3 = new Transaction(3);
            try {
                lm.lock(lockA, txn3, Lock.LockMode.EXCLUSIVE);
                System.out.printf("Thread: %d Txn3 IN CRITICAL SECTION\n", Thread.currentThread().getId());
                lm.removeTransaction(txn3);
            } catch (DeadlockException e) {
                System.out.printf("Thread: %d Txn3 Deadlock detected\n", Thread.currentThread().getId());
            }
        });

        t1.start();
        t2.start();
        Thread.sleep(200);
        t3.start();

        t1.join();
        t2.join();
        t3.join();
        System.out.printf("Thread: %d Exiting...", Thread.currentThread().getId());
    }
}
