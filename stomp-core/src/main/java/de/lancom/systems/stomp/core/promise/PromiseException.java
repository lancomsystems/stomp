package de.lancom.systems.stomp.core.promise;

/**
 * Promise exception.
 */
public class PromiseException extends RuntimeException {

    /**
     * Create new promise exception with the given cause.
     * @param cause cause
     */
    public PromiseException(final Throwable cause) {
        super(cause);
    }

}
