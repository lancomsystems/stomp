package de.lancom.systems.stomp.core.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.NonNull;

/**
 * Thread factory for named threads.
 */
public class NamedDaemonThreadFactory implements ThreadFactory {

    private final AtomicInteger threads = new AtomicInteger();
    private final String name;

    /**
     * Create a new thread factory for the given family name.
     *
     * @param name thread family name
     */
    public NamedDaemonThreadFactory(@NonNull final String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        final Thread thread = new Thread(
                runnable,
                String.format("%s %s", name, threads.incrementAndGet())
        );
        thread.setDaemon(true);
        return thread;
    }
}
