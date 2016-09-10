package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Send frame.
 */
public class SendFrame extends ClientFrame {

    /**
     * Default constructor.
     */
    public SendFrame() {
        super(StompAction.SEND.value());
    }

    /**
     * Get destination.
     *
     * @return destination
     */
    public String getDestination() {
        return this.getHeader(StompHeader.DESTINATION);
    }

    /**
     * Set destination.
     *
     * @param destination destination
     */
    public void setDestination(final String destination) {
        this.setHeader(StompHeader.DESTINATION, destination);
    }
}
