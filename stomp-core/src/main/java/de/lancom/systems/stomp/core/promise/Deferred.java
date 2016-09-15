package de.lancom.systems.stomp.core.promise;

/**
 * Asynchronous deferred.
 *
 * @param <T> result type
 */
public interface Deferred<T> {

    /**
     * Get promise.
     *
     * @return promise
     */
    Promise<T> getPromise();

    /**
     * Resolve promise.
     *
     * @param result result
     */
    void resolve(T result);

    /**
     * Reject promise.
     *
     * @param exception exception
     */
    void reject(Exception exception);

}
