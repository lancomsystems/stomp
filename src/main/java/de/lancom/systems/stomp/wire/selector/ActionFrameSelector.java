package de.lancom.systems.stomp.wire.selector;

import java.util.Objects;

import de.lancom.systems.stomp.wire.frame.Frame;

/**
 * Implementation for {@link FrameSelector} using frame action.
 */
public class ActionFrameSelector implements FrameSelector {

    private final String action;

    /**
     * Default constructor.
     *
     * @param action frame action
     */
    public ActionFrameSelector(final String action) {
        this.action = action;
    }

    @Override
    public boolean select(final Frame frame) {
        return frame != null && Objects.equals(action, frame.getAction());
    }
}
