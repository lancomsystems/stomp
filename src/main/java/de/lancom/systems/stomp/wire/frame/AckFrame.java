package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;
import de.lancom.systems.stomp.wire.StompHeader;

public class AckFrame extends ClientFrame {

    public AckFrame() {
        super(StompAction.ACK.value());
    }


    public String getId() {
        return this.getHeader(StompHeader.ID);
    }

    public void setId(final String id) {
        this.setHeader(StompHeader.ID, id);
    }
}
