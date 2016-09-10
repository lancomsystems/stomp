package de.lancom.systems.stomp.core;

/**
 * Interface for handling of {@link StompMessage}.
 */
public interface StompMessageHandler {

    boolean handle(StompMessage message);

}
