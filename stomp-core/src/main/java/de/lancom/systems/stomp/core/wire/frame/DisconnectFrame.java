package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;

/**
 * Disconnect frame.
 */
public class DisconnectFrame extends ClientFrame {

    /**
     * Create a new disconnect frame.
     */
    public DisconnectFrame() {
        super(StompAction.DISCONNECT.value());
    }

}
