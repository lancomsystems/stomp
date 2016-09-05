package de.lancom.systems.stomp.wire;

import java.util.HashMap;
import java.util.Map;

import de.lancom.systems.stomp.wire.frame.ConnectFrame;
import de.lancom.systems.stomp.wire.frame.DisconnectFrame;
import de.lancom.systems.stomp.wire.frame.Frame;
import de.lancom.systems.stomp.wire.frame.MessageFrame;
import de.lancom.systems.stomp.wire.frame.ReceiptFrame;
import de.lancom.systems.stomp.wire.frame.SendFrame;
import lombok.Getter;
import lombok.Setter;

/**
 * Context for general information and settings used in stomp communication.
 */
public class StompContext {

    private static final long DEFAULT_TIMEOUT = 2000;

    private final Map<String, Class<? extends Frame>> frameClasses = new HashMap<>();

    @Getter
    @Setter
    private long timeout = DEFAULT_TIMEOUT;

    /**
     * Default constructor.
     */
    public StompContext() {
        this.registerFrame(StompAction.CONNECT.value(), ConnectFrame.class);
        this.registerFrame(StompAction.RECEIPT.value(), ReceiptFrame.class);
        this.registerFrame(StompAction.DISCONNECT.value(), DisconnectFrame.class);
        this.registerFrame(StompAction.SEND.value(), SendFrame.class);
        this.registerFrame(StompAction.MESSAGE.value(), MessageFrame.class);
    }

    /**
     * Register frame class for a given action.
     *
     * @param action action
     * @param frameClass frame class
     */
    public void registerFrame(final String action, final Class<? extends Frame> frameClass) {
        this.frameClasses.put(action, frameClass);
    }

    /**
     * Create a new frame using the given action.
     *
     * @param action action
     * @return frame
     */
    public Frame createFrame(final String action) {
        return createFrame(action, null);
    }

    /**
     * Create a new frame using the given action and headers.
     *
     * @param action action
     * @param headers headers
     * @return frame
     */
    public Frame createFrame(final String action, final Map<String, String> headers) {
        final Class<? extends Frame> frameClass = frameClasses.get(action);
        final Frame frame;

        if (frameClass != null) {
            frame = createFrame(frameClass, headers);
        } else {
            frame = new Frame(action);
        }
        return frame;
    }

    /**
     * Create a new frame using the given frame class.
     *
     * @param frameClass frame class
     * @param <T> frame type
     * @return frame
     */
    public <T extends Frame> T createFrame(final Class<T> frameClass) {
        return createFrame(frameClass, null);
    }

    /**
     * Create a new frame using the given frame class and headers.
     *
     * @param frameClass frame class
     * @param headers headers
     * @param <T> frame type
     * @return frame
     */
    public <T extends Frame> T createFrame(final Class<T> frameClass, final Map<String, String> headers) {
        try {
            return frameClass.newInstance();
        } catch (final Exception ex) {
            throw new RuntimeException("Could not create frame parse class " + frameClass.getName(), ex);
        }
    }
}
