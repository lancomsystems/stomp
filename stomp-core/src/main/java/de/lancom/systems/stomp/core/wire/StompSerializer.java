package de.lancom.systems.stomp.core.wire;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import de.lancom.systems.stomp.core.StompContext;
import de.lancom.systems.stomp.core.util.ChannelWriter;

/**
 * Output stream for stomp {@link StompFrame}.
 */
public class StompSerializer {
    private final StompContext context;
    private final WritableByteChannel channel;
    private final ChannelWriter writer;

    /**
     * Default cosntructor.
     *
     * @param context stomp context
     * @param channel channel
     */
    public StompSerializer(final StompContext context, final WritableByteChannel channel) {
        this.context = context;
        this.channel = channel;
        this.writer = new ChannelWriter(channel, StandardCharsets.UTF_8);
    }

    /**
     * Write the given frame to the underlying output stream.
     *
     * @param frame frame
     * @throws IOException if an I/O error occurs
     */
    public synchronized void writeFrame(final StompFrame frame) throws IOException {
        if (frame != null) {
            if (frame.getBody() != null) {
                frame.setContentLength(frame.getBody().length);
            }

            writer.writeLine(frame.getAction());
            for (final Map.Entry<String, String> header : frame.getHeaders().entrySet()) {

                final String headerKey = header.getKey();
                final String headerValue;
                if (Objects.equals(frame.getAction(), StompAction.CONNECT.value())) {
                    headerValue = header.getValue();
                } else {
                    headerValue = StompEncoding.encodeHeaderValue(header.getValue());
                }

                writer.writeLine(String.format("%s:%s", headerKey, headerValue));
            }
            writer.writeLine("");
            if (frame.getBody() != null) {
                writer.write(frame.getBody());
            }
            writer.write(StompEncoding.TERMINATOR);
            writer.flush();
        }
    }

}
