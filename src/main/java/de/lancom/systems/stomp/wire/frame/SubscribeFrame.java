package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;
import de.lancom.systems.stomp.wire.StompHeader;

/**
 * Created by fkneier on 19.08.16.
 */
public class SubscribeFrame extends ClientFrame {
    public SubscribeFrame() {
        super(StompAction.SUBSCRIBE.value());
    }

    public String getId() {
        return this.getHeaders().get(StompHeader.ID.value());
    }

    public void setId(final String id) {
        this.getHeaders().put(StompHeader.ID.value(), id);
    }

    public String getDestination() {
        return this.getHeaders().get(StompHeader.DESTINATION.value());
    }

    public void setDestination(final String destination) {
        this.getHeaders().put(StompHeader.DESTINATION.value(), destination);
    }

    public String getAck() {
        return this.getHeaders().get(StompHeader.ACK.value());
    }

    public void setAck(final String ack) {
        this.getHeaders().put(StompHeader.ACK.value(), ack);
    }
}
