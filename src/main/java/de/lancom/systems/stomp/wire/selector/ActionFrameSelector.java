package de.lancom.systems.stomp.wire.selector;

import java.util.Objects;

import de.lancom.systems.stomp.wire.frame.Frame;

/**
 * Created by fkneier on 22.08.16.
 */
public class ActionFrameSelector implements FrameSelector {

    private final String action;

    public ActionFrameSelector(final String action) {
        this.action = action;
    }

    @Override
    public boolean select(final Frame frame) {
        return frame != null && Objects.equals(action, frame.getAction());
    }
}
