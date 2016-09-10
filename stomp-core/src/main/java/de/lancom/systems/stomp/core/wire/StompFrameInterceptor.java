package de.lancom.systems.stomp.core.wire;

/**
 * Interface for intercepting of {@link StompFrame} instances.
 */
public interface StompFrameInterceptor {
    /**
     * Intercept the given frame.
     *
     * @param url url
     * @param frame frame
     * @return processed frame
     * @throws Exception if an I/O error occurs
     */
    StompFrame intercept(StompUrl url, StompFrame frame) throws Exception;
}
