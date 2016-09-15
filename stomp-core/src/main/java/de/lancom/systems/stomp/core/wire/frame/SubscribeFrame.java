package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.util.EnumUtil;
import de.lancom.systems.stomp.core.wire.StompAckMode;
import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Subscribe frame.
 */
public class SubscribeFrame extends ClientFrame {

    /**
     * Create a new subscribe frame.
     */
    public SubscribeFrame() {
        super(StompAction.SUBSCRIBE.value());
    }

    /**
     * Create a new subscribe frame with given id.
     *
     * @param id id
     */
    public SubscribeFrame(final String id) {
        this();
        this.setId(id);
    }

    /**
     * Create a new subscribe frame with given id and destination.
     *
     * @param id id
     * @param destination destination
     */
    public SubscribeFrame(final String id, final String destination) {
        this(id);
        this.setDestination(destination);
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
     * Get acknowledge mode.
     *
     * @return acknowledge mode
     */
    public String getAck() {
        return this.getHeaders().get(StompHeader.ACK.value());
    }

    /**
     * Set acknowledge mode.
     *
     * @param ack acknowledge mode
     */
    public void setAck(final String ack) {
        this.getHeaders().put(StompHeader.ACK.value(), ack);
    }

    /**
     * Get acknowledge mode.
     *
     * @return acknowledge mode
     */
    public StompAckMode getAckMode() {
        return EnumUtil.findByValue(StompAckMode.class, this.getHeaders().get(StompHeader.ACK.value()));
    }

    /**
     * Set acknowledge mode.
     *
     * @param ack acknowledge mode
     */
    public void setAckMode(final StompAckMode ack) {
        this.getHeaders().put(StompHeader.ACK.value(), ack.value());
    }

    /**
     * Get selector.
     *
     * @return selector
     */
    public String getSelector() {
        return this.getHeaders().get(StompHeader.SELECTOR.value());
    }

    /**
     * Set selector.
     *
     * @param selector selector
     */
    public void setSelector(final String selector) {
        this.getHeaders().put(StompHeader.SELECTOR.value(), selector);
    }
}
