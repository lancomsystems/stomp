package de.lancom.systems.stomp.wire;

import de.lancom.systems.stomp.util.EnumValue;

/**
 * Stomp acknowledgment modes.
 */
public enum StompAckMode implements EnumValue<String> {
    /**
     * Acknowledge messages automatically.
     */
    AUTO("auto"),
    /**
     * Acknowledge all previous messages.
     */
    CLIENT("client"),
    /**
     * Acknowledge specific message.
     */
    CLIENT_INDIVIDUAL("client-individual");

    private final String value;

    /**
     * Default constructor.
     *
     * @param value enum value
     */
    StompAckMode(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
