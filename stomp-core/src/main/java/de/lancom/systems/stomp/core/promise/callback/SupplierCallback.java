package de.lancom.systems.stomp.core.promise.callback;

/**
 * Supplier callback.
 *
 * @param <R> result type
 */
public interface SupplierCallback<R> {

    /**
     * Supply value.
     *
     * @return value
     * @throws Exception if error occurs
     */
    R supply() throws Exception;

}
