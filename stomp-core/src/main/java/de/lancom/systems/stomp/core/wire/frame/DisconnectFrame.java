package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;

/**
 * Disconnect frame.
 */
public class DisconnectFrame extends ClientFrame {

    /**
     * Default constructor.
     */
    public DisconnectFrame() {
        super(StompAction.DISCONNECT.value());
    }

}
