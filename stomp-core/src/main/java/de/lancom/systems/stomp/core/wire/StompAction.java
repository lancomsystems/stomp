package de.lancom.systems.stomp.core.wire;

import de.lancom.systems.stomp.core.util.EnumValue;
import lombok.AllArgsConstructor;

/**
 * Stomp actions.
 */
@AllArgsConstructor
public enum StompAction implements EnumValue<String> {

    /**
     * Connect action.
     */
    CONNECT(StompActionSource.CLIENT),
    /**
     * Send action.
     */
    SEND(StompActionSource.CLIENT),
    /**
     * Subscribe action.
     */
    SUBSCRIBE(StompActionSource.CLIENT),
    /**
     * Unsubscribe action.
     */
    UNSUBSCRIBE(StompActionSource.CLIENT),
    /**
     * Begin action.
     */
    BEGIN(StompActionSource.CLIENT),
    /**
     * Commit action.
     */
    COMMIT(StompActionSource.CLIENT),
    /**
     * Abort action.
     */
    ABORT(StompActionSource.CLIENT),
    /**
     * Ack action.
     */
    ACK(StompActionSource.CLIENT),
    /**
     * Nack action.
     */
    NACK(StompActionSource.CLIENT),
    /**
     * Disconnect action.
     */
    DISCONNECT(StompActionSource.CLIENT),
    /**
     * Connected action.
     */
    CONNECTED(StompActionSource.SERVER),
    /**
     * Message action.
     */
    MESSAGE(StompActionSource.SERVER),
    /**
     * Receipt action.
     */
    RECEIPT(StompActionSource.SERVER),
    /**
     * Error action.
     */
    ERROR(StompActionSource.SERVER);

    private final StompActionSource source;

    @Override
    public String value() {
        return name();
    }

    /**
     * Stomp action sources.
     */
    enum StompActionSource {
        /**
         * Server.
         */
        SERVER,
        /**
         * Client.
         */
        CLIENT;
    }
}
