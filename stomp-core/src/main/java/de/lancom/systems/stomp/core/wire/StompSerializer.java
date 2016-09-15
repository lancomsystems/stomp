package de.lancom.systems.stomp.core.wire;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import de.lancom.systems.stomp.core.StompContext;

/**
 * Output stream for stomp {@link StompFrame}.
 */
public class StompSerializer {
    private final StompContext context;
    private final WritableByteChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocate(4096);

    /**
     * Default cosntructor.
     *
     * @param context stomp context
     * @param channel channel
     */
    public StompSerializer(final StompContext context, final WritableByteChannel channel) {
        this.context = context;
        this.channel = channel;
    }

    /**
     * Write the given frame to the underlying output stream.
     *
     * @param frame frame
     * @throws IOException if an I/O error occurs
     */
    public synchronized void writeFrame(final StompFrame frame) throws IOException {
        buffer.clear();
        if (frame != null) {
            if (frame.getBody() != null) {
                frame.setContentLength(frame.getBody().length);
            }

            buffer.put(frame.getAction().getBytes(StandardCharsets.UTF_8));
            buffer.put(StompEncoding.LINE_FEED);
            for (final Map.Entry<String, String> header : frame.getHeaders().entrySet()) {
                buffer.put(header.getKey().getBytes(StandardCharsets.UTF_8));
                buffer.put(StompEncoding.HEADER_SEPARATOR);
                if (Objects.equals(frame.getAction(), StompAction.CONNECT.value())) {
                    buffer.put(header.getValue().getBytes(StandardCharsets.UTF_8));
                } else {
                    buffer.put(
                            StompEncoding.encodeHeaderValue(header.getValue()).getBytes(StandardCharsets.UTF_8)
                    );
                }
                buffer.put(StompEncoding.LINE_FEED);
            }
            buffer.put(StompEncoding.LINE_FEED);
            if (frame.getBody() != null) {
                buffer.put(frame.getBody());
            }
            buffer.put(StompEncoding.TERMINATOR);
            buffer.flip();

            final int count = channel.write(buffer);
            if (count == 0) {
                throw new IOException();
            }
        }
    }

}
