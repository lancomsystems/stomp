package de.lancom.systems.stomp.core.promise.callback;

/**
 * Consumer callback.
 *
 * @param <V> value type
 */
public interface ConsumerCallback<V> {

    /**
     * Consume value.
     *
     * @param value value
     * @throws Exception if error occurs
     */
    void consume(V value) throws Exception;

}
