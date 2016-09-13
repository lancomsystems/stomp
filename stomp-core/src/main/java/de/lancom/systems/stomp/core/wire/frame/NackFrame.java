package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Nack frame.
 */
public class NackFrame extends ClientFrame {

    /**
     * Create empty nack frame.
     */
    public NackFrame() {
        super(StompAction.NACK.value());
    }

    /**
     * Create nack frame with given id.
     *
     * @param id id
     */
    public NackFrame(final String id) {
        this();
        this.setId(id);
    }

    /**
     * Create nack frame with id of the given frame.
     *
     * @param frame frame
     */
    public NackFrame(final StompFrame frame) {
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
     * @param id message id
     */
    public void setId(final String id) {
        this.setHeader(StompHeader.ID, id);
    }
}
