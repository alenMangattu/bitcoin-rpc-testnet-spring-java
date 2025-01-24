package com.coinbase.cb.configuration.ratelimit;

public enum TimeUnit {
    SECONDS(1),
    MINUTES(60),
    HOURS(3600);

    private final int seconds;

    TimeUnit(int seconds) {
        this.seconds = seconds;
    }

    public int getSeconds() {
        return seconds;
    }
} 
