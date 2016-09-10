package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompFrame;

/**
 * Base class for server frames.
 */
public abstract class ServerFrame extends StompFrame {

    /**
     * Constructor for action name.
     *
     * @param action action
     */
    public ServerFrame(final String action) {
        super(action);
    }

    /**
     * Constructor for action.
     *
     * @param action action
     */
    public ServerFrame(final StompAction action) {
        super(action.value());
    }
}
