package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Unsubscribe frame.
 */
public class UnsubscribeFrame extends ClientFrame {

    /**
     * Create a new unsubscribe frame.
     */
    public UnsubscribeFrame() {
        super(StompAction.UNSUBSCRIBE.value());
    }

    /**
     * Create a new unsubscribe frame with given id.
     *
     * @param id id
     */
    public UnsubscribeFrame(final String id) {
        this();
        this.setId(id);
    }

    /**
     * Get subscription id.
     *
     * @return subscription id
     */
    public String getId() {
        return this.getHeaders().get(StompHeader.ID.value());
    }

    /**
     * Set subscription id.
     *
     * @param id subscription id
     */
    public void setId(final String id) {
        this.getHeaders().put(StompHeader.ID.value(), id);
    }

}
