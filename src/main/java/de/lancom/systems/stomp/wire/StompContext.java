package de.lancom.systems.stomp.wire;

import java.util.HashMap;
import java.util.Map;

import de.lancom.systems.stomp.wire.frame.ConnectFrame;
import de.lancom.systems.stomp.wire.frame.DisconnectFrame;
import de.lancom.systems.stomp.wire.frame.Frame;
import de.lancom.systems.stomp.wire.frame.MessageFrame;
import de.lancom.systems.stomp.wire.frame.ReceiptFrame;
import de.lancom.systems.stomp.wire.frame.SendFrame;

public class StompContext {

    private final Map<String, Class<? extends Frame>> frameClasses = new HashMap<>();

    public StompContext() {
        this.registerFrame(StompAction.CONNECT.value(), ConnectFrame.class);
        this.registerFrame(StompAction.RECEIPT.value(), ReceiptFrame.class);
        this.registerFrame(StompAction.DISCONNECT.value(), DisconnectFrame.class);
        this.registerFrame(StompAction.SEND.value(), SendFrame.class);
        this.registerFrame(StompAction.MESSAGE.value(), MessageFrame.class);
    }

    public void registerFrame(final String action, final Class<? extends Frame> frameClass) {
        this.frameClasses.put(action, frameClass);
    }

    public Frame createFrame(final String action) {
        return createFrame(action, null);
    }
    public Frame createFrame(final String action, final Map<String, String> headers) {
        final Class<? extends Frame> frameClass = frameClasses.get(action);
        final Frame frame;

        if(frameClass != null) {
            frame = createFrame(frameClass, headers);
        } else {
            frame = new Frame(action);
        }
        return frame;
    }

    public <T extends Frame> T createFrame(final Class<T> frameClass) {
        return createFrame(frameClass, null);
    }

    public <T extends Frame> T createFrame(final Class<T> frameClass, final Map<String, String> headers) {
        try {
            return frameClass.newInstance();
        } catch (final Exception ex) {
            throw new RuntimeException("Could not create frame of class " + frameClass.getName(), ex);
        }
    }
}
