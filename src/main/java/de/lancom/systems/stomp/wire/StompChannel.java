package de.lancom.systems.stomp.wire;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.wire.frame.ConnectFrame;
import de.lancom.systems.stomp.wire.frame.ConnectedFrame;
import de.lancom.systems.stomp.wire.frame.DisconnectFrame;
import de.lancom.systems.stomp.wire.frame.Frame;
import de.lancom.systems.stomp.wire.frame.ReceiptFrame;

public class StompChannel {

    private final ByteArrayOutputStream content = new ByteArrayOutputStream();
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
    private final StompContext context;
    private final Deque<Frame> queue = new ArrayDeque<>();
    private final ReadableByteChannel readChannel;
    private final WritableByteChannel writeChannel;

    private int step = 0;
    private Integer contentLength;
    private Frame frame = null;
    private long defaultTimeout = TimeUnit.SECONDS.toMillis(10);

    public StompChannel(
            final StompContext context,
            final ByteChannel channel
    ) {
        this(context, channel, channel);
    }

    public StompChannel(
            final StompContext context,
            final ReadableByteChannel readChannel,
            final WritableByteChannel writeChannel
    ) {
        this.context = context;
        this.readChannel = readChannel;
        this.writeChannel = writeChannel;
    }

    public boolean connect() throws IOException {
        return this.connect(null);
    }

    public boolean connect(final ConnectFrame connectFrame) throws IOException {
        final ConnectFrame frame;

        if (connectFrame != null) {
            frame = connectFrame;
        } else {
            frame = new ConnectFrame();
        }

        this.writeFrame(frame);
        final Frame resultFrame = this.readFrame(defaultTimeout, TimeUnit.MILLISECONDS);
        return resultFrame instanceof ConnectedFrame;
    }

    public boolean disconnect() throws IOException {
        return this.disconnect(null);
    }

    public boolean disconnect(final DisconnectFrame disconnectFrame) throws IOException {
        final DisconnectFrame frame;

        if (disconnectFrame != null) {
            frame = disconnectFrame;
        } else {
            frame = new DisconnectFrame();
        }

        this.writeFrame(frame);
        final Frame resultFrame = this.readFrame(defaultTimeout, TimeUnit.MILLISECONDS);

        if (this.readChannel.isOpen()) {
            this.readChannel.close();
        }
        if (this.writeChannel.isOpen()) {
            this.writeChannel.close();
        }

        return resultFrame instanceof ReceiptFrame;
    }

    public Frame readFrame() throws IOException {
        return readFrame(0, TimeUnit.MILLISECONDS);
    }

    public Frame readFrame(final long timeout, final TimeUnit unit) throws IOException {
        final long start = System.currentTimeMillis();
        final long wait = unit.toMillis(timeout);

        while (this.queue.isEmpty() && readChannel.isOpen() && System.currentTimeMillis() - start <= wait) {
            this.buffer.clear();
            this.readChannel.read(this.buffer);
            this.buffer.rewind();

            if (!this.buffer.hasRemaining()) {
                this.buffer.clear();
                this.readChannel.read(this.buffer);
                this.buffer.rewind();
            }

            if (this.buffer.hasRemaining()) {
                final byte value = buffer.get();

                switch (step) {
                    case 0: {
                        if (value != StompEncoding.CARRIAGE_RETURN && value != StompEncoding.LINE_FEED) {
                            this.content.write(value);
                            this.step = 1;
                            continue;
                        } else {
                            continue;
                        }
                    }
                    case 1: {
                        if (value != StompEncoding.CARRIAGE_RETURN) {
                            if (value != StompEncoding.CARRIAGE_RETURN) {
                                this.content.write(value);
                            } else {
                                this.frame = this.context.createFrame(
                                        new String(this.content.toByteArray(), StandardCharsets.UTF_8)
                                );
                                this.content.reset();
                                this.step = 2;
                                continue;
                            }
                        }
                    }
                    case 2: {
                        if (value != StompEncoding.CARRIAGE_RETURN) {
                            if (value != StompEncoding.LINE_FEED) {
                                this.content.write(value);
                            } else {
                                if (this.content.size() != 0) {
                                    final String header = new String(
                                            this.content.toByteArray(),
                                            StandardCharsets.UTF_8
                                    );
                                    final String[] parts = header.split(":", 2);
                                    if (parts.length == 2 && frame != null) {
                                        final String headerName = parts[0];
                                        final String headerValue;
                                        if (Objects.equals(frame.getAction(), StompAction.CONNECT.value())) {
                                            headerValue = parts[1];
                                        } else {
                                            headerValue = StompEncoding.decodeHeaderValue(parts[1]);
                                        }
                                        this.frame.getHeaders().put(headerName, headerValue);
                                    }
                                    this.content.reset();
                                } else {
                                    this.contentLength = frame.getContentLength();
                                    this.step = 3;
                                    continue;
                                }
                            }
                        }
                    }
                    case 3: {
                        if (this.contentLength != null && this.contentLength > 0 || value != StompEncoding.TERMINATOR) {
                            this.content.write(value);
                            this.contentLength--;
                        } else {
                            this.queue.push(frame);
                            this.step = 0;
                            continue;
                        }
                    }
                }
            }
        }
        return queue.poll();
    }

    public Frame writeFrameForResponse(final Frame frame) throws IOException {
        return writeFrameForResponse(frame, this.defaultTimeout, TimeUnit.MILLISECONDS);
    }

    public Frame writeFrameForResponse(final Frame frame, final long timeout, final TimeUnit unit) throws IOException {
        final String receipt = UUID.randomUUID().toString();
        frame.getHeaders().put(StompHeader.RECEIPT.value(), receipt);
        this.writeFrame(frame);
        return this.readFrame(timeout, unit);
    }

    public void writeFrame(final Frame frame) throws IOException {
        if (frame != null) {
            if (frame.getBody() != null) {
                frame.setContentLength(frame.getBody().length);
            }
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(frame.getAction().getBytes(StandardCharsets.UTF_8));
            output.write(StompEncoding.LINE_FEED);
            for (final Map.Entry<String, String> header : frame.getHeaders().entrySet()) {
                output.write(header.getKey().getBytes(StandardCharsets.UTF_8));
                output.write(StompEncoding.HEADER_SEPARATOR);
                output.write(header.getValue().getBytes(StandardCharsets.UTF_8));
                output.write(StompEncoding.LINE_FEED);
            }
            output.write(StompEncoding.LINE_FEED);
            if (frame.getBody() != null) {
                output.write(frame.getBody());
            }
            output.write(StompEncoding.TERMINATOR);

            writeChannel.write(ByteBuffer.wrap(output.toByteArray()));
        }
    }

}
