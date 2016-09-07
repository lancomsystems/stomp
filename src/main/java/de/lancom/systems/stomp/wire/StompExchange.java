package de.lancom.systems.stomp.wire;

import de.lancom.systems.stomp.wire.frame.Frame;
import lombok.Data;

/**
 * Stomp message exchange.
 */
@Data
public class StompExchange {
    private final Frame request;
    private final Frame response;
}
