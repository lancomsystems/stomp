package de.lancom.systems.stomp;

import de.lancom.systems.stomp.wire.frame.Frame;

public interface StompFameHandler {
    void handle(Frame frame) throws Exception;
}
