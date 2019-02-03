package com.dstibrany.lockmanager;

public class DeadlockException extends Exception {
    DeadlockException(Exception e) {
        super(e);
    }
}
