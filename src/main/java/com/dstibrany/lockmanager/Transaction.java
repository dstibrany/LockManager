package com.dstibrany.lockmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class Transaction implements Comparable {
    private int id;
    private Thread txnThread = Thread.currentThread();
    private List<Lock> lockList = new ArrayList<>();

    Transaction(int id) {
        this.id = id;
    }

    int getId() {
        return id;
    }

    List<Lock> getLocks() {
        return lockList;
    }

    void addLock(Lock lock) {
        lockList.add(lock);
    }

    void removeLock(Lock lock) {
        lockList.remove(lock);
    }

    void abort() {
        txnThread.interrupt();
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
