package lockmanager;

class DeadlockException extends Exception {
    DeadlockException(Exception e) {
        super(e);
    }
}
