package de.lancom.systems.stomp.core.wire;

import de.lancom.systems.stomp.core.util.EnumValue;
import lombok.AllArgsConstructor;

/**
 * Stomp headers.
 */
@AllArgsConstructor
public enum StompHeader implements EnumValue<String> {

    /**
     * Id header.
     */
    ID("id"),
    /**
     * Destination header.
     */
    DESTINATION("destination"),
    /**
     * Subscription header.
     */
    SUBSCRIPTION("subscription"),
    /**
     * Message id header.
     */
    MESSAGE_ID("message-id"),
    /**
     * Receipt header.
     */
    RECEIPT("receipt"),
    /**
     * Receipt id header.
     */
    RECEIPT_ID("receipt-id"),
    /**
     * Login header.
     */
    LOGIN("login"),
    /**
     * Passcode header.
     */
    PASSCODE("passcode"),
    /**
     * Heart beat header.
     */
    HEART_BEAT("heart-beat"),
    /**
     * Content length header.
     */
    CONTENT_LENGTH("content-length"),
    /**
     * Content type header.
     */
    CONTENT_TYPE("content-type"),
    /**
     * Version header.
     */
    VERSION("version"),
    /**
     * Accept version header.
     */
    ACCEPT_VERSION("accept-version"),
    /**
     * Ack header.
     */
    ACK("ack"),
    /**
     * Selector header.
     */
    SELECTOR("selector"),
    /**
     * Persistence header.
     */
    PERSISTENCE("persistent");

    private final String value;

    @Override
    public String value() {
        return value;
    }
}
