package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;
import de.lancom.systems.stomp.wire.StompHeader;

/**
 * Created by fkneier on 19.08.16.
 */
public class UnsubscribeFrame extends ClientFrame {

    public UnsubscribeFrame() {
        super(StompAction.UNSUBSCRIBE.value());
    }

    public String getId() {
        return this.getHeaders().get(StompHeader.ID.value());
    }

    public void setId(final String id) {
        this.getHeaders().put(StompHeader.ID.value(), id);
    }

}
