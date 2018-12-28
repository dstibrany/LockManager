package lockmanager;

import java.util.Objects;

class Transaction implements Comparable {
    private int id;

    Transaction(int id) {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id == that.id;
    }

    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(Object o) {
        Transaction that = (Transaction) o;
        return this.id - that.id;
    }

    int getId() {
        return id;
    }
}
