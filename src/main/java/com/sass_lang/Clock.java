package com.sass_lang;

import java.util.Date;

public class Clock {
    private static Clock delegate = new Clock();

    public static void setDelegate(Clock clock) {
        delegate = clock;
    }

    public static Date now() {
        return delegate.getCurrentTime();
    }

    protected Date getCurrentTime() {
        return new Date();
    }
}
