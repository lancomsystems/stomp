package de.lancom.systems.stomp.core.connection;

/**
 * Stomp frame callback.
 */
public interface StompFrameContextInterceptor {
    /**
     * Intercept stomp frame context.
     *
     * @param context context
     * @throws Exception if an error occurs
     */
    void intercept(StompFrameContext context) throws Exception;
}
