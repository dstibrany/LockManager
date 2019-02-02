package com.dstibrany.lockmanager;

class DeadlockException extends Exception {
    DeadlockException(Exception e) {
        super(e);
    }
}
