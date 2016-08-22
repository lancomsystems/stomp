package de.lancom.systems.stomp.wire;

import de.lancom.systems.stomp.util.EnumValue;

public enum  StompAckMode implements EnumValue<String> {
    AUTO("auto"), CLIENT("client"), CLIENT_INDIVIDUAL("client-individual");

    private final String value;

    StompAckMode(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
