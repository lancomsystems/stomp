package de.lancom.systems.stomp.wire;

import de.lancom.systems.stomp.util.EnumValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StompHeader implements EnumValue<String> {

    ID("id"),
    DESTINATION("destination"),
    SUBSCRIPTION("subscription"),
    MESSAGE_ID("message-id"),
    RECEIPT("receipt"),
    RECEIPT_ID("receipt-id"),
    LOGIN("login"),
    PASSCODE("passcode"),
    HEART_BEAT("heart-beat"),
    CONTENT_LENGTH("content-length"),
    CONTENT_TYPE("content-type"),
    VERSION("accept-version"),
    ACCEPT_VERSION("accept-version"),
    ACK("ack");

    private final String value;

    @Override
    public String value() {
        return value;
    }
}
