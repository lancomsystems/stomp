package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;
import de.lancom.systems.stomp.wire.StompHeader;

public class ConnectedFrame extends ServerFrame {

    public ConnectedFrame() {
        super(StompAction.CONNECTED.value());
    }


    public String getVersion() {
        return this.getHeaders().get(StompHeader.VERSION.value());
    }

    public void setVersion(final String version) {
        this.getHeaders().put(StompHeader.VERSION.value(), version);
    }

}
