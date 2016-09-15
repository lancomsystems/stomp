package de.lancom.systems.stomp.core.promise.callback;

import de.lancom.systems.stomp.core.promise.Promise;

/**
 * Supplier callback.
 *
 * @param <R> result type
 */
public interface PromiseSupplierCallback<R> {

    /**
     * Supply value.
     *
     * @return promise
     * @throws Exception if error occurs
     */
    Promise<R> supply() throws Exception;

}
