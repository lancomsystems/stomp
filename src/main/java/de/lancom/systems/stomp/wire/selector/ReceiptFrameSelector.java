package de.lancom.systems.stomp.wire.selector;

import java.util.Objects;

import de.lancom.systems.stomp.wire.StompHeader;
import de.lancom.systems.stomp.wire.frame.ClientFrame;
import de.lancom.systems.stomp.wire.frame.Frame;

/**
 * Implementation for {@link FrameSelector} using frame receipt id.
 */
public class ReceiptFrameSelector implements FrameSelector {

    private final String receipt;

    /**
     * Default constructor.
     *
     * @param receipt receipt id
     */
    public ReceiptFrameSelector(final String receipt) {
        this.receipt = receipt;
    }

    /**
     * Constructor using receipt id from {@link ClientFrame}.
     *
     * @param frame client frame
     */
    public ReceiptFrameSelector(final ClientFrame frame) {
        frame.setRandomReceipt();
        this.receipt = frame.getReceipt();
    }

    @Override
    public boolean select(final Frame frame) {
        return frame != null && Objects.equals(receipt, frame.getHeaders().get(StompHeader.RECEIPT_ID.value()));
    }

}
