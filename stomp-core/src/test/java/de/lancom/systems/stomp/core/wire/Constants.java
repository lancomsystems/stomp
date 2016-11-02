package de.lancom.systems.stomp.core.wire;

import de.lancom.systems.stomp.core.StompContext;
import de.lancom.systems.stomp.test.StompBroker;

public class Constants {
    public static final StompBroker BROKER = new StompBroker();
    public static final StompContext CONTEXT = new StompContext();
    public static final int TIMEOUT_SECONDS = 10;

    static {
        try {
            BROKER.start();
            CONTEXT.start();
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
