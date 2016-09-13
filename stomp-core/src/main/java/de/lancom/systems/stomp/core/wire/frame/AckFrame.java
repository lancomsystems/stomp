package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Ack frame.
 */
public class AckFrame extends ClientFrame {

    /**
     * Create empty nack frame.
     */
    public AckFrame() {
        super(StompAction.ACK.value());
    }

    /**
     * Create ack frame with given id.
     *
     * @param id id
     */
    public AckFrame(final String id) {
        this();
        this.setId(id);
    }

    /**
     * Create ack frame with id of the given frame.
     *
     * @param frame frame
     */
    public AckFrame(final StompFrame frame) {
        this(frame.getHeader(StompHeader.ACK));
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
     * @param id id
     */
    public void setId(final String id) {
        this.setHeader(StompHeader.ID, id);
    }
}
