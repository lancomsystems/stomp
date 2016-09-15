package de.lancom.systems.stomp.core.wire.frame;

import java.util.UUID;

import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Base class for client frames.
 */
public abstract class ClientFrame extends StompFrame {

    /**
     * Create a new client frame.
     *
     * @param action action name
     */
    public ClientFrame(final String action) {
        super(action);
    }

    /**
     * Get receipt id.
     *
     * @return receipt id
     */
    public String getReceipt() {
        return this.getHeaders().get(StompHeader.RECEIPT.value());
    }

    /**
     * Set receipt id.
     *
     * @param receipt receipt id
     */
    public void setReceipt(final String receipt) {
        this.getHeaders().put(StompHeader.RECEIPT.value(), receipt);
    }

    /**
     * Set a random receipt id.
     * Id is generated using a random UUID string
     */
    public void setRandomReceipt() {
        this.setReceipt(UUID.randomUUID().toString());
    }

}
