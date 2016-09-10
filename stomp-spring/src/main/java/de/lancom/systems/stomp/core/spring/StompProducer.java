package de.lancom.systems.stomp.core.spring;

import java.util.concurrent.CompletableFuture;

/**
 * Stomp producer interface.
 *
 * @param <T> value type
 */
public interface StompProducer<T> {

    /**
     * Send stomp frame.
     *
     * @param value value
     * @return stomp exchange
     */
    CompletableFuture<Void> send(T value);

}
