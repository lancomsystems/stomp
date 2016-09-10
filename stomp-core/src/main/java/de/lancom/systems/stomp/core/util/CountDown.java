package de.lancom.systems.stomp.core.util;

import java.util.concurrent.TimeUnit;

import lombok.Getter;

/**
 * Utility class for timeout count down.
 */
public class CountDown {

    @Getter
    private final long start;

    @Getter
    private long amount;

    /**
     * Default constructor.
     *
     * @param amount amount
     * @param unit time unit
     */
    public CountDown(final long amount, final TimeUnit unit) {
        this.start = System.currentTimeMillis();
        this.amount = unit.toMillis(amount);
    }

    /**
     * Calculate the remaining microseconds.
     *
     * @return remaining microseconds
     */
    public long remaining() {
        return amount - (System.currentTimeMillis() - start);
    }
}
