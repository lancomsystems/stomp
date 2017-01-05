package de.lancom.systems.stomp.core.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import de.lancom.systems.stomp.core.wire.StompEncoding;

/**
 * Helper class for channel writing.
 */
public class ChannelWriter {

    private static final int BUFFER_SIZE = 4096;

    private final WritableByteChannel channel;
    private final Charset charset;
    private ByteBuffer buffer;

    /**
     * Create a new writer.
     *
     * @param channel channel
     * @param charset charset
     */
    public ChannelWriter(final WritableByteChannel channel, final Charset charset) {
        this.channel = channel;
        this.charset = charset;
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    /**
     * Write line.
     *
     * @param line line
     * @throws IOException on io error
     */
    public void writeLine(final String line) throws IOException {
        this.write(line.getBytes(charset));
        this.write(StompEncoding.LINE_FEED);
    }

    /**
     * Write block.
     *
     * @param block block
     * @throws IOException on io error
     */
    public void writeBlock(final byte[] block) throws IOException {
        this.write(block);
        this.write(StompEncoding.TERMINATOR);
    }

    /**
     * Write single byte.
     *
     * @param value value
     * @throws IOException on io error
     */
    public void write(final byte value) throws IOException {
        if (this.buffer.remaining() < 1) {
            this.flush();
        }
        this.buffer.put(value);
    }

    /**
     * Write byte array.
     *
     * @param array array
     * @throws IOException on io error
     */
    public void write(final byte[] array) throws IOException {
        int position = 0;
        while (position < array.length) {
            final int step = Math.min(array.length - position, this.buffer.remaining());
            this.buffer.put(array, position, step);
            position += step;
            if (this.buffer.remaining() == 0) {
                this.flush();
            }
        }
    }

    /**
     * Flush buffer to channel.
     *
     * @throws IOException on io error
     */
    public void flush() throws IOException {
        if (this.buffer.position() > 0) {
            this.buffer.flip();
            this.channel.write(this.buffer);
            this.buffer.clear();
        }
    }

}
