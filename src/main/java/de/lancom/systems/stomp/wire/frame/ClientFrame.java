package de.lancom.systems.stomp.wire.frame;

import java.util.UUID;

import de.lancom.systems.stomp.wire.StompHeader;

public abstract class ClientFrame extends Frame {

    public ClientFrame(final String action) {
        super(action);
    }

    public String getReceipt() {
        return this.getHeaders().get(StompHeader.RECEIPT.value());
    }

    public void setReceipt(final String receipt) {
        this.getHeaders().put(StompHeader.RECEIPT.value(), receipt);
    }

    public void setRandomReceipt() {
        this.setReceipt(UUID.randomUUID().toString());
    }

}
