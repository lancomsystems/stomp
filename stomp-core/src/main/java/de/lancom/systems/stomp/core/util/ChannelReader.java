package de.lancom.systems.stomp.core.util;

import static de.lancom.systems.stomp.core.util.StringUtil.isBlank;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import de.lancom.systems.stomp.core.wire.StompEncoding;

/**
 * Helper class for channel reading.
 */
public class ChannelReader {

    private static final int BUFFER_SIZE = 4096;

    private final ReadableByteChannel channel;
    private final CharsetDecoder decoder;
    private ByteBuffer buffer;

    /**
     * Create a new buffer.
     *
     * @param channel channel
     * @param charset charset
     */
    public ChannelReader(final ReadableByteChannel channel, final Charset charset) {
        this.channel = channel;
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.buffer.limit(0);
        this.decoder = charset.newDecoder();
    }

    /**
     * Read block of given or unknown length.
     *
     * @param length expected length
     * @return block
     * @throws IOException on io error
     */
    public byte[] readBlock(final Integer length) throws IOException {

        int end = -1;

        if (length != null) {
            end = this.buffer.position() + length;
            if (!this.fill(end)) {
                end = -1;
            }
        } else {

            int position;
            for (position = this.buffer.position(); end == -1 && this.fill(position); position++) {
                if (this.buffer.get(position) == StompEncoding.TERMINATOR) {
                    end = position;
                }
            }
        }

        if (end != -1) {
            final byte[] result = new byte[end - this.buffer.position()];
            if (result.length > 0) {
                this.buffer.get(result);
            }
            this.buffer.position(Math.min(end + 1, this.buffer.limit()));
            return result;
        } else {
            return null;
        }

    }

    /**
     * Read a single line.
     *
     * @return line
     * @throws IOException on io error
     */

    public String readLine() throws IOException {

        int end = -1;
        int position;
        for (position = this.buffer.position(); end == -1 && this.fill(position); position++) {
            if (this.buffer.get(position) == StompEncoding.LINE_FEED) {
                boolean preceedingCr = true;
                preceedingCr = preceedingCr && position > this.buffer.position();
                preceedingCr = preceedingCr && this.buffer.get(position - 1) == StompEncoding.CARRIAGE_RETURN;

                if (preceedingCr) {
                    end = position - 1;
                } else {
                    end = position;
                }

            } else {
                end = -1;
            }

        }

        if (end != -1) {
            final ByteBuffer contentBuffer = this.buffer.duplicate();
            contentBuffer.limit(end);
            final String result = this.decoder.decode(contentBuffer).toString();
            this.buffer.position(position);
            return result;
        } else {
            return null;
        }

    }

    /**
     * Discards all remaining empty lines.
     *
     * @throws IOException on io error
     */
    public void discardEmptyLines() throws IOException {
        int last;
        do {
            last = this.buffer.position();
        } while (isBlank(this.readLine()));
        this.buffer.position(last);
    }

    /**
     * Fill buffer if required.
     *
     * @param position position
     * @return success
     * @throws IOException on io error
     */
    private boolean fill(final int position) throws IOException {

        if (position >= this.buffer.limit() && this.channel.isOpen()) {
            final int oldPosition = this.buffer.position();
            if (position >= buffer.capacity()) {
                final ByteBuffer oldBuffer = this.buffer;
                oldBuffer.rewind();
                this.buffer = ByteBuffer.allocate((int) Math.ceil((double) position / BUFFER_SIZE) * BUFFER_SIZE);
                this.buffer.put(oldBuffer);
                this.buffer.position(oldBuffer.position());
                this.buffer.limit(oldBuffer.limit());
            }
            this.buffer.position(this.buffer.limit());
            this.buffer.limit(this.buffer.capacity());
            this.channel.read(this.buffer);
            this.buffer.flip();
            this.buffer.position(oldPosition);
        }

        return this.buffer.limit() > position;

    }

    /**
     * Rewind buffer.
     */
    public void rewind() {
        this.buffer.rewind();
    }

    /**
     * Discard content that has been read so far.
     */
    public void next() {
        this.buffer.compact();
        this.buffer.limit(this.buffer.position());
        this.buffer.rewind();
    }

}
