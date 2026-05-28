package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

public class SpeedyPartialFailure {
    private final int index;
    private final int status;
    private final String message;
    private final String timestamp;
    private final SpeedyEntityKey inputPk;

    public SpeedyPartialFailure(int index, int status, String message,
                                 String timestamp, SpeedyEntityKey inputPk) {
        this.index = index;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.inputPk = inputPk;
    }

    public int getIndex()              { return index; }
    public int getStatus()             { return status; }
    public String getMessage()         { return message; }
    public String getTimestamp()       { return timestamp; }
    public SpeedyEntityKey getInputPk() { return inputPk; }
}
