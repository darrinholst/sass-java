package com.sass_lang;

import java.util.Date;

public class FakeClock extends Clock {
    protected Date stopped = new Date();

    @Override
    protected Date getCurrentTime() {
        return stopped; 
    }

    public void incrementSeconds(int seconds) {
        stopped = new Date(stopped.getTime() + (seconds * 1000));
    }
}
