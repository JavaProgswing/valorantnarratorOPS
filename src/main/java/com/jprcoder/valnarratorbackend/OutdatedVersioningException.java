package com.jprcoder.valnarratorbackend;

public class OutdatedVersioningException extends Exception {
    public OutdatedVersioningException() {
        super();
    }

    public OutdatedVersioningException(String message) {
        super(message);
    }

    public OutdatedVersioningException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutdatedVersioningException(Throwable cause) {
        super(cause);
    }
}
