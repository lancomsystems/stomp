package de.lancom.systems.stomp.wire.selector;

import de.lancom.systems.stomp.wire.frame.Frame;

/**
 * Interface for frame selection.
 */
public interface FrameSelector {

    /**
     * Callback function for frame selection.
     *
     * @param frame frame
     * @return selection flag
     */
    boolean select(Frame frame);

}
