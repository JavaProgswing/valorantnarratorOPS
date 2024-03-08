package com.jprcoder.valnarratorbackend;

public class ResponseProcess {
    private boolean isFinished = false;

    public boolean isRunning() {
        return !isFinished;
    }

    public void setFinished() throws Exception {
        if (isFinished) throw new Exception("ResponseProcess has already finished!");
        isFinished = true;
    }

    public void reset() {
        isFinished = false;
    }
}
