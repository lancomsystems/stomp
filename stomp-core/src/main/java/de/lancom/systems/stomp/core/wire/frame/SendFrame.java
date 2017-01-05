package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Send frame.
 */
public class SendFrame extends ClientFrame {

    /**
     * Create a new send frame.
     */
    public SendFrame() {
        super(StompAction.SEND.value());
        this.setPersistence(true);
    }

    /**
     * Create a new send frame with destination.
     *
     * @param destination destination
     */
    public SendFrame(final String destination) {
        this();
        this.setDestination(destination);
    }

    /**
     * Create a new send frame with destination and body.
     *
     * @param destination destination
     * @param body body
     */
    public SendFrame(final String destination, final String body) {
        this(destination);
        this.setBodyAsString(body);
    }

    /**
     * Create a new send frame with destination and body.
     *
     * @param destination destination
     * @param body body
     */
    public SendFrame(final String destination, final byte[] body) {
        this(destination);
        this.setBody(body);
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

    /**
     * Get message persistence.
     *
     * @return persistence
     */
    public Boolean getPersistence() {
        final String header = this.getHeader(StompHeader.PERSISTENCE);
        if (header != null) {
            return header.equals("true");
        } else {
            return null;
        }
    }

    /**
     * Set message persistence.
     *
     * @param persistence persistence
     */
    public void setPersistence(final Boolean persistence) {
        if (persistence != null) {
            this.setHeader(StompHeader.PERSISTENCE, persistence.toString());
        } else {
            this.removeHeader(StompHeader.PERSISTENCE);
        }
    }
}
