package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;

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
