package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;
import de.lancom.systems.stomp.wire.StompHeader;

public class MessageFrame extends ServerFrame {

    public MessageFrame() {
        super(StompAction.MESSAGE.value());
    }

    public String getSubscription() {
        return this.getHeaders().get(StompHeader.SUBSCRIPTION.value());
    }

    public void setSubscription(final String subscription) {
        this.getHeaders().put(StompHeader.SUBSCRIPTION.value(), subscription);
    }

    public String getMessageId() {
        return this.getHeader(StompHeader.MESSAGE_ID);
    }

    public void setMessageId(final String messageId) {
        this.setHeader(StompHeader.MESSAGE_ID, messageId);
    }

    public String getAck() {
        return this.getHeader(StompHeader.ACK);
    }

    public void setAck(final String ack) {
        this.setHeader(StompHeader.ACK, ack);
    }
}
