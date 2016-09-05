package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;
import de.lancom.systems.stomp.wire.StompHeader;

/**
 * Subscribe frame.
 */
public class SubscribeFrame extends ClientFrame {

    /**
     * Default constructor.
     */
    public SubscribeFrame() {
        super(StompAction.SUBSCRIBE.value());
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

    /**
     * Get destination.
     *
     * @return destination
     */
    public String getDestination() {
        return this.getHeaders().get(StompHeader.DESTINATION.value());
    }

    /**
     * Set destination.
     *
     * @param destination destination
     */
    public void setDestination(final String destination) {
        this.getHeaders().put(StompHeader.DESTINATION.value(), destination);
    }

    /**
     * Get acknowledge id.
     *
     * @return acknowledge id
     */
    public String getAck() {
        return this.getHeaders().get(StompHeader.ACK.value());
    }

    /**
     * Set acknowledge id.
     *
     * @param ack acknowledge id
     */
    public void setAck(final String ack) {
        this.getHeaders().put(StompHeader.ACK.value(), ack);
    }
}
