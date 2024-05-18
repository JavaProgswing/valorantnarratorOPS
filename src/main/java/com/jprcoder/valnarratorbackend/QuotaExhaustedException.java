package com.jprcoder.valnarratorbackend;

public class QuotaExhaustedException extends Exception {
    public QuotaExhaustedException(long refreshesIn) {
        super(String.format("refreshes in %d s.", refreshesIn));
    }
}
