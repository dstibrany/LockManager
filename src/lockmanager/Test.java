package lockmanager;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        LockManager lm = new LockManager();
        Thread t1 = new Thread(new ThreadAction(lm, 1, 55, "X"));
        Thread t2 = new Thread(new ThreadAction(lm, 2, 55, "S"));
        Thread t3 = new Thread(new ThreadAction(lm, 3, 55, "S"));
        Thread t4 = new Thread(new ThreadAction(lm, 4, 55, "X"));
        Thread t5 = new Thread(new ThreadAction(lm, 5, 55, "S"));
        Thread t6 = new Thread(new ThreadAction(lm, 6, 55, "S"));
        t1.start();
        Thread.sleep(20);
        t2.start();
        Thread.sleep(20);
        t3.start();
        Thread.sleep(20);
        t4.start();
        Thread.sleep(20);
        t5.start();
        Thread.sleep(20);
        t6.start();
    }
}

class ThreadAction implements Runnable {
    private LockManager lm;
    private int txn;
    private int obj;
    private String mode;

    ThreadAction(LockManager lm, int txn, int obj, String mode) {
        this.lm = lm;
        this.txn = txn;
        this.obj = obj;
        this.mode = mode;
    }

    public void run() {
        System.out.println("Txn:" + txn + " lock request");
        lm.lock(obj, txn, mode);
        System.out.println("Txn:" + txn + " has the lock");
        try {
            Thread.sleep(1400);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Txn:" + txn + " releasing the lock");
        lm.removeTransaction(txn);
    }
}