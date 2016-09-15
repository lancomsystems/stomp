package de.lancom.systems.stomp.core.connection;

/**
 * Stomp frame callback.
 */
public interface StompFrameContextHandler {
    /**
     * Handler given stomp frame context.
     *
     * @param context context
     * @return handled
     * @throws Exception if an error occurs
     */
    boolean handle(StompFrameContext context) throws Exception;
}
