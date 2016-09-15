package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Message frame.
 */
public class MessageFrame extends ServerFrame {

    /**
     * Create a new message frame.
     */
    public MessageFrame() {
        super(StompAction.MESSAGE);
    }

    /**
     * Get subscription id.
     *
     * @return subscription id
     */
    public String getSubscription() {
        return this.getHeader(StompHeader.SUBSCRIPTION);
    }

    /**
     * Set subscription id.
     *
     * @param subscription subscription id
     */
    public void setSubscription(final String subscription) {
        this.setHeader(StompHeader.SUBSCRIPTION, subscription);
    }

    /**
     * Get message id.
     *
     * @return message id
     */
    public String getMessageId() {
        return this.getHeader(StompHeader.MESSAGE_ID);
    }

    /**
     * Set message id.
     *
     * @param messageId message id
     */
    public void setMessageId(final String messageId) {
        this.setHeader(StompHeader.MESSAGE_ID, messageId);
    }

    /**
     * Get acknowledge id.
     *
     * @return acklowledge id
     */
    public String getAck() {
        return this.getHeader(StompHeader.ACK);
    }

    /**
     * Set acknowleged id.
     *
     * @param ack acknowledge id
     */
    public void setAck(final String ack) {
        this.setHeader(StompHeader.ACK, ack);
    }
}
