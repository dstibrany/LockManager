package com.dstibrany.lockmanager;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class Transaction implements Comparable {
    private int id;
    private Set<Lock> lockList = new HashSet<>();

    Transaction(int id) {
        this.id = id;
    }

    int getId() {
        return id;
    }

    Set<Lock> getLocks() {
        return lockList;
    }

    void addLock(Lock lock) {
        lockList.add(lock);
    }

    void removeLock(Lock lock) {
        lockList.remove(lock);
    }

    void abort() {
        Thread.currentThread().interrupt();
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
