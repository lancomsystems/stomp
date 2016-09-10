package de.lancom.systems.stomp.core;

import de.lancom.systems.stomp.core.util.EnumValue;

/**
 * Defined message meta data names.
 */
public enum StompMessageMeta implements EnumValue<String> {

    MESSAGE_RECEIPT, MESSAGE_RESPONSE, MESSAGE_ACK;

    @Override
    public String value() {
        return name();
    }

}
