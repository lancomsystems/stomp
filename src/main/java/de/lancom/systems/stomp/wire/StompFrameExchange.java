package de.lancom.systems.stomp.wire;

import de.lancom.systems.stomp.wire.frame.Frame;
import lombok.Data;

/**
 * Exchange holder for stomp request and response frames.
 */
@Data
public class StompFrameExchange {
    private final Frame request;
    private final Frame response;
}
