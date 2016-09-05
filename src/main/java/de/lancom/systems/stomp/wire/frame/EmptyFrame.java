package de.lancom.systems.stomp.wire.frame;


/**
 * Empty frame.
 * This frame is not official and is used for empty responses in {@link de.lancom.systems.stomp.wire.StompFameExchange}.
 */
public class EmptyFrame extends Frame {

    /**
     * Default constructor.
     */
    public EmptyFrame() {
        super("EMPTY");
    }

}
