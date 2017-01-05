package de.lancom.systems.stomp.core.wire;

import de.lancom.systems.stomp.core.util.EnumValue;
import lombok.AllArgsConstructor;

/**
 * Active MQ specific stomp headers.
 */
@AllArgsConstructor
public enum StompActiveMqHeader implements EnumValue<String> {

    /**
     * Prefetch size header.
     */
    PREFETCH_SIZE("activemq.prefetchSize");

    private final String value;

    @Override
    public String value() {
        return value;
    }
}
