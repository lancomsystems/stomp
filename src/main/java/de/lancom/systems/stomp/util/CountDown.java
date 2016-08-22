package de.lancom.systems.stomp.util;

import java.util.concurrent.TimeUnit;

/**
 * Created by fkneier on 22.08.16.
 */
public class CountDown {
    final long start;
    long value;

    public CountDown(final long amount, final TimeUnit unit) {
        this.start = System.currentTimeMillis();
        this.value = unit.toMillis(amount);
    }

    public long remaining() {
        return value - (System.currentTimeMillis() - start);
    }
}
