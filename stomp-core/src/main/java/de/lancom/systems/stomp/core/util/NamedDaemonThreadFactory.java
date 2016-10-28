package de.lancom.systems.stomp.core.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread factory for named threads.
 */
@Slf4j
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
        final Thread thread = new LoggedThread(
                runnable,
                String.format("%s %s", name, threads.incrementAndGet())
        );
        thread.setDaemon(true);

        return thread;
    }

    /**
     * Thread instance that does additional logging.
     */
    private static class LoggedThread extends Thread {

        /**
         * Create a new instance.
         *
         * @param target runnable
         * @param name name
         */
        LoggedThread(final Runnable target, final String name) {
            super(target, name);
            log.debug("Created thread " + this.getName());
        }

        @Override
        public synchronized void start() {
            log.debug("Starting thread " + this.getName());
            super.start();
        }

        @Override
        public void run() {
            log.debug("Running thread " + this.getName());
            super.run();
            log.debug("Finished thread " + this.getName());
        }
    }
}
