package de.lancom.systems.stomp;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.lancom.systems.stomp.util.CountDown;
import de.lancom.systems.stomp.wire.StompContext;
import de.lancom.systems.stomp.wire.StompExchange;
import de.lancom.systems.stomp.wire.StompInputStream;
import de.lancom.systems.stomp.wire.StompOutputStream;
import de.lancom.systems.stomp.wire.StompUrl;
import de.lancom.systems.stomp.wire.frame.AckFrame;
import de.lancom.systems.stomp.wire.frame.ConnectFrame;
import de.lancom.systems.stomp.wire.frame.DisconnectFrame;
import de.lancom.systems.stomp.wire.frame.EmptyFrame;
import de.lancom.systems.stomp.wire.frame.Frame;
import de.lancom.systems.stomp.wire.frame.MessageFrame;
import de.lancom.systems.stomp.wire.frame.NackFrame;
import de.lancom.systems.stomp.wire.frame.SendFrame;
import de.lancom.systems.stomp.wire.frame.SubscribeFrame;
import de.lancom.systems.stomp.wire.frame.UnsubscribeFrame;
import de.lancom.systems.stomp.wire.selector.FrameSelector;
import de.lancom.systems.stomp.wire.selector.ReceiptFrameSelector;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StompClient {

    private final ExecutorService receiverExecutor = Executors.newSingleThreadExecutor(
            new NamedThreadFactory("StompReceiver")
    );
    private final ExecutorService senderExecutor = Executors.newSingleThreadExecutor(
            new NamedThreadFactory("StompSender")
    );
    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(
            new NamedThreadFactory("StompConsumer")
    );
    private final List<Connection> connections = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Getter
    @Setter
    private long timeout = 2000;

    @Getter
    private final StompContext context = new StompContext();

    public void start() {
        if (running.compareAndSet(false, true)) {
            this.receiverExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    if (log.isDebugEnabled()) {
                        log.debug("Stomp client receiver started");
                    }
                    while (running.get()) {
                        for (final Connection connection : connections) {
                            try {
                                final Frame frame = connection.getInputStream().readFrame();
                                if (frame != null) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Got " + frame);
                                    }

                                    boolean handled = false;
                                    if (frame instanceof MessageFrame) {
                                        final MessageFrame messageFrame = (MessageFrame) frame;
                                        final String subscriptionId = ((MessageFrame) frame).getSubscription();
                                        final Subscription subscription = connection.getSubscriptionMap().get(
                                                subscriptionId
                                        );

                                        if (subscription != null) {
                                            StompClient.this.consumerExecutor.execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        subscription.getHandler().handle(messageFrame);
                                                        if (messageFrame.getAck() != null) {
                                                            StompClient.this.ack(
                                                                    connection.getBase(), messageFrame
                                                            ).get();
                                                        }
                                                    } catch (final Exception ex) {
                                                        if (log.isErrorEnabled()) {
                                                            log.error("Failed to process  " + frame, ex);
                                                        }
                                                    }
                                                }
                                            });
                                            handled = true;
                                        }

                                    }

                                    if (!handled) {
                                        final List<Frame> queue = connection.getIncomingQueue();
                                        synchronized (queue) {
                                            queue.add(frame);
                                            queue.notifyAll();
                                        }
                                    }
                                }
                            } catch (final Exception ex) {
                                if (log.isErrorEnabled()) {
                                    log.error("Failed to read messages from " + connection.getBase(), ex);
                                }
                            }
                        }
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Stomp client receiver stopped");
                    }
                }
            });
        }
    }

    public void stop() throws IOException {
        if (running.compareAndSet(true, false)) {

            final List<Future<StompExchange>> futures = new ArrayList<>();

            for (final Connection connection : connections) {

                for (final Subscription subscription : connection.getSubscriptionMap().values()) {
                    futures.add(this.unsubscribe(connection.getBase(), subscription.getId()));
                }

                final DisconnectFrame disconnectFrame = context.createFrame(DisconnectFrame.class);
                disconnectFrame.setRandomReceipt();
                futures.add(this.exchange(
                        connection,
                        disconnectFrame,
                        new ReceiptFrameSelector(disconnectFrame.getReceipt())
                ));
            }

            for (final Future<StompExchange> future : futures) {
                try {
                    future.get();
                } catch (final Exception ex) {
                    if (log.isWarnEnabled()) {
                        log.warn("Disconnect failed", ex);
                    }
                }
            }

            try {
                this.senderExecutor.shutdown();
                this.senderExecutor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (final InterruptedException ex) {
                if (log.isWarnEnabled()) {
                    log.warn("Failed to shut down sender", ex);
                }
            }

            try {
                this.receiverExecutor.shutdown();
                this.receiverExecutor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (final InterruptedException ex) {
                if (log.isWarnEnabled()) {
                    log.warn("Failed to shut down receiver", ex);
                }
            }

            for (final Connection connection : connections) {
                try {
                    connection.getInputStream().close();
                    connection.getOutputStream().close();
                } catch (final Exception ex) {
                    if (log.isWarnEnabled()) {
                        log.warn("Could not close connection to " + connection.getBase(), ex);
                    }
                }
            }
        }
    }

    public Connection getConnection(final StompUrl url, final boolean create) throws IOException {
        final StompUrl base = url.getBase();

        for (final Connection connection : connections) {
            if (Objects.equals(base, connection.getBase())) {
                return connection;
            }
        }
        if (create) {
            final Connection connection = new Connection(
                    base,
                    context,
                    new Socket(url.getHost(), url.getPort())
            );
            final ConnectFrame connectFrame = this.context.createFrame(ConnectFrame.class);
            connection.getOutputStream().writeFrame(connectFrame);
            final Frame frame = connection.getInputStream().readFrame(timeout, TimeUnit.MILLISECONDS);
            this.connections.add(connection);
            return connection;
        }

        return null;
    }

    public Future<StompExchange> send(
            final StompUrl url,
            final String body
    ) throws IOException {
        return send(url, body, null);
    }

    public Future<StompExchange> send(
            final StompUrl url,
            final String body,
            final Map<String, String> headers
    ) throws IOException {
        final SendFrame frame = this.context.createFrame(SendFrame.class, headers);
        frame.setDestination(url.getDestination());
        frame.setBodyAsString(body);
        frame.setRandomReceipt();

        return this.send(url, frame);
    }

    public Future<StompExchange> send(
            final StompUrl url,
            final SendFrame sendFrame
    ) throws IOException {
        return this.exchange(
                this.getConnection(url, true),
                sendFrame,
                new ReceiptFrameSelector(sendFrame)
        );
    }

    public Future<StompExchange> subscribe(
            final StompUrl url,
            final String id,
            final StompFameHandler handler
    ) throws IOException {

        final SubscribeFrame frame = this.context.createFrame(SubscribeFrame.class);
        frame.setRandomReceipt();
        frame.setDestination(url.getDestination());
        frame.setId(id);

        return this.subscribe(url, frame, handler);
    }

    public Future<StompExchange> subscribe(
            final StompUrl url,
            final SubscribeFrame subscribeFrame,
            final StompFameHandler handler
    ) throws IOException {
        final Connection connection = this.getConnection(url, true);
        connection.getSubscriptionMap().put(subscribeFrame.getId(), new Subscription(subscribeFrame.getId(), handler));

        return this.exchange(
                connection,
                subscribeFrame,
                new ReceiptFrameSelector(subscribeFrame)
        );
    }

    public Future<StompExchange> unsubscribe(
            final StompUrl url,
            final String id
    ) throws IOException {

        final Connection connection = this.getConnection(url, true);
        final Subscription subscription = connection.getSubscriptionMap().get(id);
        if (subscription != null) {
            final UnsubscribeFrame frame = this.context.createFrame(UnsubscribeFrame.class);
            frame.setId(id);
            frame.setRandomReceipt();

            return unsubscribe(url, frame);
        }
        return null;
    }

    public Future<StompExchange> unsubscribe(
            final StompUrl url,
            final UnsubscribeFrame unsubscribeFrame
    ) throws IOException {
        return this.exchange(
                this.getConnection(url, true),
                unsubscribeFrame,
                new ReceiptFrameSelector(unsubscribeFrame)
        );
    }

    public Future<StompExchange> ack(final StompUrl url, final MessageFrame frame) throws IOException {
        final AckFrame ackFrame = this.context.createFrame(AckFrame.class);
        ackFrame.setId(frame.getAck());
        ackFrame.setRandomReceipt();

        return ack(url, ackFrame);
    }

    public Future<StompExchange> ack(final StompUrl url, final AckFrame ackFrame) throws IOException {
        return this.exchange(
                this.getConnection(url, true),
                ackFrame,
                new ReceiptFrameSelector(ackFrame.getReceipt())
        );
    }

    public Future<StompExchange> nack(
            final StompUrl url,
            final MessageFrame frame
    ) throws IOException {
        final NackFrame nackFrame = this.context.createFrame(NackFrame.class);
        nackFrame.setId(frame.getMessageId());

        return nack(url, nackFrame);
    }

    public Future<StompExchange> nack(
            final StompUrl url,
            final NackFrame nackFrame
    ) throws IOException {

        return this.exchange(
                this.getConnection(url, true),
                nackFrame,
                new ReceiptFrameSelector(nackFrame)
        );
    }

    private Future<StompExchange> exchange(
            final Connection connection,
            final Frame frame,
            final FrameSelector selector
    ) {
        if (log.isDebugEnabled()) {
            log.debug("Sending " + frame);
        }

        return senderExecutor.submit(new Callable<StompExchange>() {
            @Override
            public StompExchange call() throws Exception {
                connection.getOutputStream().writeFrame(frame);

                if (selector != null) {
                    final CountDown countDown = new CountDown(StompClient.this.timeout, TimeUnit.MILLISECONDS);
                    while (countDown.remaining() > 0) {
                        final Iterator<Frame> iterator = connection.incomingQueue.iterator();
                        while (iterator.hasNext()) {
                            final Frame next = iterator.next();
                            if (selector.select(next)) {
                                return new StompExchange(frame, next);
                            }
                        }
                        final List<Frame> queue = connection.getIncomingQueue();
                        synchronized (queue) {
                            queue.wait(countDown.remaining());
                        }
                    }
                    return null;
                } else {
                    return new StompExchange(frame, new EmptyFrame());
                }

            }
        });
    }

    @Data
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private class Subscription {

        @Getter
        private final String id;

        @Getter
        private final StompFameHandler handler;

    }

    @Data
    private static class Connection {

        private final StompUrl base;

        @Getter
        private final StompInputStream inputStream;

        @Getter
        private final StompOutputStream outputStream;

        @Getter
        private final Map<String, Subscription> subscriptionMap = new ConcurrentSkipListMap<>();

        @Getter
        private List<Frame> incomingQueue = new CopyOnWriteArrayList<>();

        private Connection(
                final StompUrl base,
                final StompContext context,
                final Socket socket
        ) throws IOException {
            this.base = base;
            this.inputStream = new StompInputStream(context, socket.getInputStream());
            this.outputStream = new StompOutputStream(context, socket.getOutputStream());
        }

    }

    private static class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger();
        private final String name;

        public NamedThreadFactory(final String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, String.format("%s-%s", name, counter.incrementAndGet()));
        }
    }

}
