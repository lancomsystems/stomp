package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Receipt frame.
 */
public class ReceiptFrame extends ServerFrame {

    /**
     * Default constructor.
     */
    public ReceiptFrame() {
        super(StompAction.RECEIPT.value());
    }

    /**
     * Get receipt id.
     *
     * @return receipt id
     */
    public String getReceiptId() {
        return this.getHeaders().get(StompHeader.RECEIPT.value());
    }

    /**
     * Set receipt id.
     *
     * @param receiptId receipt id
     */
    public void setReceiptId(final String receiptId) {
        this.getHeaders().put(StompHeader.RECEIPT.value(), receiptId);
    }

}
