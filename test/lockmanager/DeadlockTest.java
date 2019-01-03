package lockmanager;

import net.jodah.concurrentunit.Waiter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class DeadlockTest {

    @Test
    void twoWayDeadLock() {
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
                lm.removeTransaction(txn1);
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
                lm.removeTransaction(txn2);
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

    @Test
    void threeWayDeadlock() {
        LockManager lm = new LockManager();
        int lockA = 99;
        int lockB = 100;
        int lockC = 101;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Waiter waiter3 = new Waiter();

        Thread t1 = new Thread(() -> {
            Transaction txn1 = new Transaction(1);
            try {
                lm.lock(lockA, txn1, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(100);
                lm.lock(lockB, txn1, Lock.LockMode.EXCLUSIVE);
                lm.removeTransaction(txn1);
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
                lm.lock(lockC, txn2, Lock.LockMode.EXCLUSIVE);
                lm.removeTransaction(txn2);
                waiter2.resume();
            } catch (DeadlockException e) {
                waiter2.fail("Txn2 should not have been aborted by the deadlock detector");
            } catch (InterruptedException e) {}
        });

        Thread t3 = new Thread(() -> {
            Transaction txn3 = new Transaction(3);
            try {
                lm.lock(lockC, txn3, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(100);
                lm.lock(lockA, txn3, Lock.LockMode.EXCLUSIVE);
                lm.removeTransaction(txn3);
                waiter3.fail("Txn3 should have been aborted by the deadlock detector");
            } catch (DeadlockException e) {
                waiter3.resume();
            } catch (InterruptedException e) {}
        });

        t1.start();
        t2.start();
        t3.start();

        try {
            waiter1.await(2000);
            waiter2.await(2000);
            waiter3.await(2000);
        } catch (TimeoutException e) {
            fail("Deadlock was not resolved");
        }
    }
    @Test
    void nonDeadlockedTransactionGetsUnblocked() throws Exception {
        LockManager lm = new LockManager();
        int lockA = 99;
        int lockB = 100;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Waiter waiter3 = new Waiter();

        Thread t1 = new Thread(() -> {
            Transaction txn1 = new Transaction(1);
            try {
                lm.lock(lockA, txn1, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(100);
                lm.lock(lockB, txn1, Lock.LockMode.EXCLUSIVE);
                lm.removeTransaction(txn1);
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
                lm.removeTransaction(txn2);
                waiter2.fail("Txn2 should have been aborted by the deadlock detector");
            } catch (DeadlockException e) {
                waiter2.resume();
            } catch (InterruptedException e) {}
        });

        Thread t3 = new Thread(() -> {
            Transaction txn3 = new Transaction(3);
            try {
                lm.lock(lockB, txn3, Lock.LockMode.EXCLUSIVE);
                lm.removeTransaction(txn3);
                waiter3.resume();
            } catch (DeadlockException e) {
                waiter3.fail("Txn 3 should not have been aborted");
            }
        });

        t1.start();
        t2.start();
        Thread.sleep(20);
        t3.start();

        try {
            waiter1.await(2000);
            waiter2.await(2000);
            waiter3.await(2000);
        } catch (TimeoutException e) {
            fail("Deadlock was not resolved");
        }
    }

    @Test
    void notAbortedIfNotDeadlocked() {
        LockManager lm = new LockManager();
        int lockA = 99;
        int lockB = 100;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();

        Thread t1 = new Thread(() -> {
            Transaction txn1 = new Transaction(1);
            try {
                lm.lock(lockA, txn1, Lock.LockMode.SHARED);
                lm.removeTransaction(txn1);
            } catch (DeadlockException e) {
                waiter1.resume();
            }
        });
        Thread t2 = new Thread(() -> {
            Transaction txn2 = new Transaction(2);
            try {
                lm.lock(lockB, txn2, Lock.LockMode.SHARED);
                lm.removeTransaction(txn2);
            } catch (DeadlockException e) {
                waiter2.resume();
            }
        });

        t1.start();
        t2.start();

        assertThrows(TimeoutException.class, () -> {
            waiter1.await(1200);
            waiter2.await(1200);
        }, "Deadlock detector should not have aborted either transaction");
    }

    @Test
    void multipleDeadlocks() {
        LockManager lm = new LockManager();
        int lockA = 99;
        int lockB = 100;
        int lockC = 101;
        int lockD = 102;
        Waiter waiter1 = new Waiter();
        Waiter waiter2 = new Waiter();
        Waiter waiter3 = new Waiter();
        Waiter waiter4 = new Waiter();

        Thread t1 = new Thread(() -> {
            Transaction txn1 = new Transaction(1);
            try {
                lm.lock(lockA, txn1, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(100);
                lm.lock(lockB, txn1, Lock.LockMode.EXCLUSIVE);
                lm.removeTransaction(txn1);
                waiter1.resume();
            } catch (DeadlockException e) {
                waiter1.fail("Txn1 should not have been aborted by DL detector");
            } catch (Exception e) {}
        });
        Thread t2 = new Thread(() -> {
            Transaction txn2 = new Transaction(2);
            try {
                lm.lock(lockB, txn2, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(100);
                lm.lock(lockA, txn2, Lock.LockMode.EXCLUSIVE);
                lm.removeTransaction(txn2);
                waiter2.fail("Txn2 should have been aborted by DL detector");
            } catch (DeadlockException e) {
                waiter2.resume();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread t3 = new Thread(() -> {
            Transaction txn3 = new Transaction(3);
            try {
                lm.lock(lockC, txn3, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(100);
                lm.lock(lockD, txn3, Lock.LockMode.EXCLUSIVE);
                lm.removeTransaction(txn3);
                waiter3.resume();
            } catch (DeadlockException e) {
                waiter3.fail("Txn3 should not have been aborted by DL detector");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread t4 = new Thread(() -> {
            Transaction txn4 = new Transaction(4);
            try {
                lm.lock(lockD, txn4, Lock.LockMode.EXCLUSIVE);
                Thread.sleep(100);
                lm.lock(lockC, txn4, Lock.LockMode.EXCLUSIVE);
                lm.removeTransaction(txn4);
                waiter4.fail("Txn4 should have been aborted by DL detector");
            } catch (DeadlockException e) {
                waiter4.resume();
            } catch (Exception e) {}
        });

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        try {
            waiter1.await(1500);
            waiter2.await(1500);
        } catch (TimeoutException e) {
            fail("Deadlock was not properly detected between txn1 and txn2");
        }

        try {
            waiter3.await(1500);
            waiter4.await(1500);
        } catch (TimeoutException e) {
            fail("Deadlock was not properly detected between txn3 and txn4");
        }
    }
}
