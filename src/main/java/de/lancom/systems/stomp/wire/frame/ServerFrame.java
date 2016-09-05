package de.lancom.systems.stomp.wire.frame;

/**
 * Base class for server frames.
 */
public abstract class ServerFrame extends Frame {

    /**
     * Default constructor.
     *
     * @param action action
     */
    public ServerFrame(final String action) {
        super(action);
    }

}
