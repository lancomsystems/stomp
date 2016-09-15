package de.lancom.systems.stomp.spring;

import de.lancom.systems.stomp.core.connection.StompFrameContext;
import de.lancom.systems.stomp.core.promise.Promise;

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
    Promise<StompFrameContext> send(T value);

}
