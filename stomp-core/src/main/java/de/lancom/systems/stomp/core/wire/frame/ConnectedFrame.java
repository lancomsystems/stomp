package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Connected frame.
 */
public class ConnectedFrame extends ServerFrame {

    /**
     * Create an ew connected frame.
     */
    public ConnectedFrame() {
        super(StompAction.CONNECTED.value());
    }

    /**
     * Get server stomp version.
     *
     * @return stomp version
     */
    public String getVersion() {
        return this.getHeaders().get(StompHeader.VERSION.value());
    }

    /**
     * Set server stomp version.
     *
     * @param version stomp version
     */
    public void setVersion(final String version) {
        this.getHeaders().put(StompHeader.VERSION.value(), version);
    }

}
