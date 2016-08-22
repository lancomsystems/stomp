package de.lancom.systems.stomp.wire;

import de.lancom.systems.stomp.wire.frame.Frame;
import lombok.Data;

/**
 * Created by fkneier on 22.08.16.
 */
@Data
public class StompExchange {
    private final Frame request;
    private final Frame response;
}
