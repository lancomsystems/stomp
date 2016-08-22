package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;

public class DisconnectFrame extends ClientFrame {

    public DisconnectFrame() {
        super(StompAction.DISCONNECT.value());
    }

}
