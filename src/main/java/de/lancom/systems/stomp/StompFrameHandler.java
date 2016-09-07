package de.lancom.systems.stomp;

import de.lancom.systems.stomp.wire.frame.Frame;

/**
 * Interface for handling of {@link Frame}.
 */
public interface StompFrameHandler {
    /**
     * Handler the given frame.
     *
     * @param frame frame
     * @throws Exception if an I/O error occurs
     */
    void handle(Frame frame) throws Exception;
}
