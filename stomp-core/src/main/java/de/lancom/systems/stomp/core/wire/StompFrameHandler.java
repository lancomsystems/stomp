package de.lancom.systems.stomp.core.wire;

/**
 * Interface for handling of {@link StompFrame}.
 */
public interface StompFrameHandler {

    /**
     * Handler the given frame.
     *
     * @param url url
     * @param frame frame
     * @return message processed successfully
     * @throws Exception if an I/O error occurs
     */
    boolean handle(StompUrl url, StompFrame frame) throws Exception;

}
