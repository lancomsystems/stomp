package de.lancom.systems.stomp.core.promise.callback;

import de.lancom.systems.stomp.core.promise.Promise;

/**
 * Processor callback.
 *
 * @param <V> value type
 * @param <R> result type
 */
public interface PromiseProcessorCallback<V, R> {

    /**
     * Process value.
     *
     * @param value value
     * @return promise
     * @throws Exception if error occurs
     */
    Promise<R> process(V value) throws Exception;

}
