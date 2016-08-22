package de.lancom.systems.stomp.wire.selector;

import de.lancom.systems.stomp.wire.frame.Frame;

/**
 * Created by fkneier on 22.08.16.
 */
public interface FrameSelector {
    boolean select(Frame frame);
}
