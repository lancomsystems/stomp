package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Receipt frame.
 */
public class ReceiptFrame extends ServerFrame {

    /**
     * Create empty receipt frame.
     */
    public ReceiptFrame() {
        super(StompAction.RECEIPT.value());
    }

    /**
     * Create receipt with id of the given frame.
     *
     * @param id id
     */
    public ReceiptFrame(final String id) {
        this();
        this.setReceiptId(id);
    }

    /**
     * Create receipt with id of the given frame.
     *
     * @param frame frame
     */
    public ReceiptFrame(final StompFrame frame) {
        this(frame.getHeader(StompHeader.RECEIPT));
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
