package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompFrame;
import lombok.NonNull;

/**
 * Base class for server frames.
 */
public abstract class ServerFrame extends StompFrame {

    /**
     * Constructor for action name.
     *
     * @param action action
     */
    public ServerFrame(@NonNull final String action) {
        super(action);
    }

    /**
     * Constructor for action.
     *
     * @param action action
     */
    public ServerFrame(@NonNull final StompAction action) {
        super(action.value());
    }
}
