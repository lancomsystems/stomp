package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;
import de.lancom.systems.stomp.wire.StompHeader;

/**
 * Nack frame.
 */
public class NackFrame extends ClientFrame {

    /**
     * Default constructor.
     */
    public NackFrame() {
        super(StompAction.ACK.value());
    }

    /**
     * Get message id.
     *
     * @return message id
     */
    public String getId() {
        return this.getHeader(StompHeader.ID);
    }

    /**
     * Set message id.
     *
     * @param id message id
     */
    public void setId(final String id) {
        this.setHeader(StompHeader.ID, id);
    }
}
