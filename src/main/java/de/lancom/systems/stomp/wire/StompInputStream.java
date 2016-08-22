package de.lancom.systems.stomp.wire;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import de.lancom.systems.stomp.util.CountDown;
import de.lancom.systems.stomp.wire.frame.Frame;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StompInputStream extends InputStream {
    private final ReentrantLock lock = new ReentrantLock();
    private final StompContext context;
    private final InputStream inputStream;
    private final BufferedReader reader;

    public StompInputStream(final StompContext context, final InputStream inputStream) {
        this.context = context;
        this.inputStream = inputStream;
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public Frame readFrame() throws IOException {
        return readFrame(0, TimeUnit.MILLISECONDS);
    }

    public Frame readFrame(final long timeout, final TimeUnit unit) throws IOException {
        final CountDown countDown = new CountDown(timeout, unit);

        Frame frame = null;
        boolean locked;
        try {
            locked = lock.tryLock(countDown.remaining(), TimeUnit.MILLISECONDS);
            if (locked) {
                // read action
                do {
                    final long readStart = System.currentTimeMillis();
                    String actionLine = null;
                    while (reader.ready() && (actionLine = reader.readLine()) != null) {
                        if (isEmpty(actionLine)) {
                            continue;
                        } else {
                            frame = context.createFrame(actionLine);
                            break;
                        }
                    }
                } while (countDown.remaining() > 0 && frame == null);

                if (frame != null) {
                    // read headers
                    String headerLine = null;
                    while (!isEmpty(headerLine = reader.readLine())) {
                        final String[] parts = headerLine.split(":", 2);
                        if (parts.length == 2 && frame != null) {
                            final String headerName = parts[0];
                            final String headerValue;
                            if (Objects.equals(frame.getAction(), StompAction.CONNECT.value())) {
                                headerValue = parts[1];
                            } else {
                                headerValue = StompEncoding.decodeHeaderValue(parts[1]);
                            }

                            frame.getHeaders().put(headerName, headerValue);
                        }
                    }

                    // read body
                    final Integer contentLength = frame.getContentLength();
                    final CharArrayWriter writer = new CharArrayWriter();
                    while (reader.ready()) {
                        final int value = reader.read();

                        final boolean more;
                        if (contentLength != null) {
                            more = contentLength > writer.size();
                        } else {
                            more = value != 0;
                        }
                        if (more) {
                            writer.write(value);
                        } else {
                            break;
                        }
                    }

                    final char[] body = writer.toCharArray();
                    if (body != null && body.length > 0) {
                        frame.setBodyAsString(new String(body));
                    }
                }
            }
        } catch (final Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Error reading messages", ex);
            }
        } finally {
            lock.unlock();
        }

        return frame;
    }

    private boolean isEmpty(final String value) {
        if (value != null) {
            for (int index = 0; index < value.length(); index++) {
                if (!Character.isWhitespace(value.charAt(index))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public synchronized void mark(final int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }
}
