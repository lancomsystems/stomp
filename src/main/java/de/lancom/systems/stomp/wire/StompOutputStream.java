package de.lancom.systems.stomp.wire;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import de.lancom.systems.stomp.wire.frame.Frame;

/**
 * Output stream for stomp {@link Frame}.
 */
public class StompOutputStream extends OutputStream {
    private final ReentrantLock lock = new ReentrantLock();
    private final StompContext context;
    private final OutputStream outputStream;

    /**
     * Default cosntructor.
     *
     * @param context stomp context
     * @param outputStream base output stream
     */
    public StompOutputStream(final StompContext context, final OutputStream outputStream) {
        this.context = context;
        this.outputStream = outputStream;
    }

    /**
     * Write the given frame to the underlying output stream.
     * @param frame frame
     * @throws IOException if an I/O error occurs
     */
    public void writeFrame(final Frame frame) throws IOException {
        try {
            lock.lock();
            if (frame != null) {
                if (frame.getBody() != null) {
                    frame.setContentLength(frame.getBody().length);
                }

                outputStream.write(frame.getAction().getBytes(StandardCharsets.UTF_8));
                outputStream.write(StompEncoding.LINE_FEED);
                for (final Map.Entry<String, String> header : frame.getHeaders().entrySet()) {
                    outputStream.write(header.getKey().getBytes(StandardCharsets.UTF_8));
                    outputStream.write(StompEncoding.HEADER_SEPARATOR);
                    outputStream.write(header.getValue().getBytes(StandardCharsets.UTF_8));
                    outputStream.write(StompEncoding.LINE_FEED);
                }
                outputStream.write(StompEncoding.LINE_FEED);
                if (frame.getBody() != null) {
                    outputStream.write(frame.getBody());
                }
                outputStream.write(StompEncoding.TERMINATOR);
                outputStream.flush();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void write(final int b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        outputStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
