package lockmanager;

import java.util.Objects;

class Transaction implements Comparable {
    private int id;
    private boolean aborted = false;
    private Thread txnThread = Thread.currentThread();

    Transaction(int id) {
        this.id = id;
    }

    int getId() {
        return id;
    }

    void abort() {
        aborted = true;
        System.out.printf("Thread: %d Txn: %d aborting\n", Thread.currentThread().getId(), getId());
        txnThread.interrupt();

    }

    boolean isAborted() {
        return aborted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(Object o) {
        Transaction that = (Transaction) o;
        return this.id - that.id;
    }
}
