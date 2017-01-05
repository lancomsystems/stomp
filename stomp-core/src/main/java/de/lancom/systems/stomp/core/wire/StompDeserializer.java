package de.lancom.systems.stomp.core.wire;

import static de.lancom.systems.stomp.core.util.StringUtil.isBlank;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import de.lancom.systems.stomp.core.StompContext;
import de.lancom.systems.stomp.core.util.ChannelReader;
import lombok.extern.slf4j.Slf4j;

/**
 * Input stream for stomp {@link StompFrame}.
 */
@Slf4j
public class StompDeserializer {
    private final StompContext context;
    private final ReadableByteChannel channel;
    private final ChannelReader reader;

    /**
     * Default constructor.
     *
     * @param context stomp context
     * @param channel channel
     */
    public StompDeserializer(final StompContext context, final ReadableByteChannel channel) {
        this.context = context;
        this.channel = channel;
        this.reader = new ChannelReader(channel, StandardCharsets.UTF_8);
    }

    /**
     * Read frame from underlying input stream.
     *
     * @return frame or null if none is available
     * @throws IOException if an I/O error occurs
     */
    public synchronized StompFrame readFrame() throws IOException {
        StompFrame frame;

        // read action
        while (true) {
            final String line = this.reader.readLine();
            if (line == null) {
                frame = null;
                break;
            } else if (isBlank(line)) {
                continue;
            } else {
                frame = context.createFrame(line);
                break;
            }
        }

        // read headers
        if (frame != null) {
            while (true) {
                final String line = this.reader.readLine();

                if (isBlank(line)) {
                    break;
                } else {
                    final String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        final String headerName = parts[0];
                        final String headerValue;
                        if (Objects.equals(frame.getAction(), StompAction.CONNECT.value())) {
                            headerValue = parts[1];
                        } else {
                            headerValue = StompEncoding.decodeHeaderValue(parts[1]);
                        }

                        frame.getHeaders().put(headerName, headerValue);
                    } else {
                        throw new RuntimeException(String.format("Error reading frame header line '%s'", line));
                    }
                }
            }
        }

        // read body
        if (frame != null) {
            final byte[] body = this.reader.readBlock(frame.getContentLength());
            if (body != null) {
                if (body.length > 0) {
                    frame.setBody(body);
                }
            } else {
                frame = null;
            }

            this.reader.next();
        }

        if (frame == null) {
            this.reader.rewind();
        }

        return frame;
    }

}
