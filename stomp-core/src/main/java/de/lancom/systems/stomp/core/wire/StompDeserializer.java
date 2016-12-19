package de.lancom.systems.stomp.core.wire;

import static de.lancom.systems.stomp.core.util.StringUtil.isBlank;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import de.lancom.systems.stomp.core.StompContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Input stream for stomp {@link StompFrame}.
 */
@Slf4j
public class StompDeserializer {
    private final StompContext context;
    private final ReadableByteChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
    private final ByteBufferReader reader = new ByteBufferReader(buffer, StandardCharsets.UTF_8);

    /**
     * Default constructor.
     *
     * @param context stomp context
     * @param channel channel
     */
    public StompDeserializer(final StompContext context, final ReadableByteChannel channel) {
        this.context = context;
        this.channel = channel;
    }

    /**
     * Read frame from underlying input stream.
     *
     * @return frame or null if none is available
     * @throws IOException if an I/O error occurs
     */
    public synchronized StompFrame readFrame() throws IOException {
        StompFrame frame = null;

        reader.reset();
        while (frame == null && channel.isOpen() && buffer.position() + channel.read(buffer) > 0) {
            // read action
            while (true) {
                final String line = reader.readLine();
                if (line == null) {
                    return null;
                } else if (isBlank(line)) {
                    continue;
                } else {
                    frame = context.createFrame(line);
                    break;
                }
            }

            if (frame != null) {
                // read headers
                while (true) {
                    final String line = reader.readLine();

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

                // read body
                final byte[] body = reader.readBlock(frame.getContentLength());
                if (body != null) {
                    if (body.length > 0) {
                        frame.setBody(body);
                    }
                } else {
                    return null;
                }

                reader.discardEmptyLines();

                buffer.limit(buffer.position());
                if (reader.getPosition() <= buffer.limit()) {
                    buffer.position(reader.getPosition());
                } else {
                    log.debug("Unable to apply new buffer position ({} > {}). Body: ",
                            reader.getPosition(),
                            buffer.limit());
                }

                buffer.compact();

                reader.reset();

                return frame;
            }
        }

        return null;
    }

    /**
     * Helper class for frame parsing.
     */
    @RequiredArgsConstructor
    public static class ByteBufferReader {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private final ByteBuffer buffer;
        private final Charset charset;

        @Getter
        @Setter
        private int position = 0;

        /**
         * Read a single line.
         *
         * @return line
         */
        public String readLine() {
            int current = position - 1;

            outputStream.reset();
            while (++current < buffer.limit()) {
                final byte value = buffer.get(current);

                if (value == StompEncoding.CARRIAGE_RETURN) {
                    continue;
                }
                if (value != StompEncoding.LINE_FEED) {
                    outputStream.write(value);
                } else {
                    position = current + 1;
                    return new String(outputStream.toByteArray(), charset);
                }
            }

            return null;
        }

        /**
         * Read block of given length.
         *
         * @param length expected length
         * @return block
         */
        public byte[] readBlock(final Integer length) {
            int current = position - 1;

            outputStream.reset();
            while (++current < buffer.limit()) {
                final byte value = buffer.get(current);

                final int relative = current - position;
                if (length != null && relative < length || value != StompEncoding.TERMINATOR) {
                    outputStream.write(value);
                } else {
                    position = current + 1;
                    return outputStream.toByteArray();
                }
            }

            return null;
        }

        /**
         * Discards all remaining empty lines.
         */
        public void discardEmptyLines() {
            String line = null;
            do {
                final int previous = position;
                line = readLine();
                if (!isBlank(line)) {
                    position = previous;
                    break;
                }
            } while (line != null);
        }

        /**
         * Reset position.
         */
        public void reset() {
            this.position = 0;
        }

    }

}
