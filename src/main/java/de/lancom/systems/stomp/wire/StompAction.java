package de.lancom.systems.stomp.wire;

import de.lancom.systems.stomp.util.EnumValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StompAction implements EnumValue<String> {

    CONNECT(StompActionSource.CLIENT),
    SEND(StompActionSource.CLIENT),
    SUBSCRIBE(StompActionSource.CLIENT),
    UNSUBSCRIBE(StompActionSource.CLIENT),
    BEGIN(StompActionSource.CLIENT),
    COMMIT(StompActionSource.CLIENT),
    ABORT(StompActionSource.CLIENT),
    ACK(StompActionSource.CLIENT),
    NACK(StompActionSource.CLIENT),
    DISCONNECT(StompActionSource.CLIENT),
    CONNECTED(StompActionSource.SERVER),
    MESSAGE(StompActionSource.SERVER),
    RECEIPT(StompActionSource.SERVER),
    ERROR(StompActionSource.SERVER);

    private final StompActionSource source;

    public String value() {
        return name();
    }

    enum StompActionSource {
        SERVER,
        CLIENT;
    }
}
