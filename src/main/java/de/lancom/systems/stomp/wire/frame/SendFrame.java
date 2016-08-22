package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;
import de.lancom.systems.stomp.wire.StompHeader;

/**
 * Created by fkneier on 18.08.16.
 */
public class SendFrame extends ClientFrame {

    public SendFrame() {
        super(StompAction.SEND.value());
    }


    public String getDestination() {
        return this.getHeader(StompHeader.DESTINATION);
    }

    public void setDestination(final String destination) {
        this.setHeader(StompHeader.DESTINATION, destination);
    }
}
