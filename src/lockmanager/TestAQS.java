package lockmanager;

public class TestAQS {
    public static void main(String args[]) throws InterruptedException {
        LockAQS lock = new LockAQS();

        Thread t1 = new Thread(new ThreadActionAQS(lock, 1));
        Thread t2 = new Thread(new ThreadActionAQS(lock, 2));
        t1.start();
        Thread.sleep(20);
        t2.start();
    }
}

class ThreadActionAQS implements Runnable {
    private LockAQS lock;
    private int txn;

    ThreadActionAQS(LockAQS lock, int txn) {
        this.lock = lock;
        this.txn = txn;
    }

    public void run() {
        System.out.println("Txn:" + txn + " lock request");
        lock.lock();
        System.out.println("Txn:" + txn + " has the lock");
        try {
            Thread.sleep(1400);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Txn:" + txn + " releasing the lock");
        lock.unlock();
    }
}
