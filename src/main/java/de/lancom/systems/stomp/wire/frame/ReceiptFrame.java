package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;
import de.lancom.systems.stomp.wire.StompHeader;

public class ReceiptFrame extends ServerFrame {

    public ReceiptFrame() {
        super(StompAction.RECEIPT.value());
    }

    public String getReceiptId() {
        return this.getHeaders().get(StompHeader.RECEIPT.value());
    }

    public void setSubscription(final String subscription) {
        this.getHeaders().put(StompHeader.SUBSCRIPTION.value(), subscription);
    }

}
