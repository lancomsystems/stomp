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
import de.lancom.systems.stomp.wire.StompFameExchange;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Stomp client.
 * This client allows several concurrent connections to different servers using stomp urls.
 */
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
    private final StompContext context = new StompContext();

    /**
     * Start the client and listen for new frames.
     */
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

    /**
     * Stop the client and close all connections.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {

            final List<Future<StompFameExchange>> futures = new ArrayList<>();

            for (final Connection connection : connections) {

                for (final Subscription subscription : connection.getSubscriptionMap().values()) {
                    try {
                        futures.add(this.unsubscribe(connection.getBase(), subscription.getId()));
                    } catch (final Exception ex) {
                        if (log.isWarnEnabled()) {
                            log.warn("Could not unsubscribe " + subscription.getId(), ex);
                        }
                    }

                }

                final DisconnectFrame disconnectFrame = context.createFrame(DisconnectFrame.class);
                disconnectFrame.setRandomReceipt();
                futures.add(this.exchange(
                        connection,
                        disconnectFrame,
                        new ReceiptFrameSelector(disconnectFrame.getReceipt())
                ));
            }

            for (final Future<StompFameExchange> future : futures) {
                if (future != null) {
                    try {
                        future.get();
                    } catch (final Exception ex) {
                        if (log.isWarnEnabled()) {
                            log.warn("Disconnect failed", ex);
                        }
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

    /**
     * Send the given message body to the given url and wait for response if expected.
     *
     * @param url url
     * @param body body
     * @return frame exchange
     * @throws IOException if an I/O error occurs
     */
    public Future<StompFameExchange> send(
            final StompUrl url,
            final String body
    ) throws IOException {
        return send(url, body, null);
    }

    /**
     * Send the given message body to the given url with the given headers and wait for response if expected.
     *
     * @param url url
     * @param body body
     * @param headers headers
     * @return frame exchange
     * @throws IOException if an I/O error occurs
     */
    public Future<StompFameExchange> send(
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

    /**
     * Send the given send frame to the given url and wait for response if expected.
     *
     * @param url url
     * @param sendFrame frame
     * @return frame exchange
     * @throws IOException if an I/O error occurs
     */
    public Future<StompFameExchange> send(
            final StompUrl url,
            final SendFrame sendFrame
    ) throws IOException {
        return this.exchange(
                this.getConnection(url, true),
                sendFrame,
                new ReceiptFrameSelector(sendFrame)
        );
    }

    /**
     * Subscribe to the given url using the given id and frame handler.
     *
     * @param url url
     * @param id id
     * @param handler frame handler
     * @return frame exchange
     * @throws IOException if an I/O error occurs
     */
    public Future<StompFameExchange> subscribe(
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

    /**
     * Subscribe to the given url using the given subscribe frame and frame handler.
     *
     * @param url url
     * @param subscribeFrame subscribe frame
     * @param handler frame handler
     * @return frame exchange
     * @throws IOException if an I/O error occurs
     */
    public Future<StompFameExchange> subscribe(
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

    /**
     * Unsubscribe from the given url using the given id.
     *
     * @param url url
     * @param id id
     * @return frame exchange
     * @throws IOException if an I/O error occurs
     */
    public Future<StompFameExchange> unsubscribe(
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

    /**
     * Unsubscribe from the given url using the given unsubscribe frame.
     *
     * @param url url
     * @param unsubscribeFrame unsubscribe frame
     * @return frame exchange
     * @throws IOException if an I/O error occurs
     */
    public Future<StompFameExchange> unsubscribe(
            final StompUrl url,
            final UnsubscribeFrame unsubscribeFrame
    ) throws IOException {
        return this.exchange(
                this.getConnection(url, true),
                unsubscribeFrame,
                new ReceiptFrameSelector(unsubscribeFrame)
        );
    }

    /**
     * Send ack for the given message frame.
     *
     * @param url url
     * @param frame frame
     * @return frame exchange
     * @throws IOException if an I/O error occurs
     */
    public Future<StompFameExchange> ack(final StompUrl url, final MessageFrame frame) throws IOException {
        final AckFrame ackFrame = this.context.createFrame(AckFrame.class);
        ackFrame.setId(frame.getAck());
        ackFrame.setRandomReceipt();

        return ack(url, ackFrame);
    }

    /**
     * Send ack using the given ack frame.
     *
     * @param url url
     * @param ackFrame ack frame
     * @return frame exchange
     * @throws IOException if an I/O error occurs
     */
    public Future<StompFameExchange> ack(final StompUrl url, final AckFrame ackFrame) throws IOException {
        return this.exchange(
                this.getConnection(url, true),
                ackFrame,
                new ReceiptFrameSelector(ackFrame.getReceipt())
        );
    }

    /**
     * Send nack for the given message frame.
     *
     * @param url url
     * @param frame frame
     * @return frame exchange
     * @throws IOException if an I/O error occurs
     */
    public Future<StompFameExchange> nack(
            final StompUrl url,
            final MessageFrame frame
    ) throws IOException {
        final NackFrame nackFrame = this.context.createFrame(NackFrame.class);
        nackFrame.setId(frame.getMessageId());

        return nack(url, nackFrame);
    }

    /**
     * Send nack using the given ack frame.
     *
     * @param url url
     * @param nackFrame nack frame
     * @return frame exchange
     * @throws IOException if an I/O error occurs
     */
    public Future<StompFameExchange> nack(
            final StompUrl url,
            final NackFrame nackFrame
    ) throws IOException {

        return this.exchange(
                this.getConnection(url, true),
                nackFrame,
                new ReceiptFrameSelector(nackFrame)
        );
    }

    /**
     * Find connection for the given url or create a new one.
     *
     * @param url url
     * @param create creation flag
     * @return connection or null if none exists and should not be created
     * @throws IOException if an I/O error occurs
     */
    private Connection getConnection(final StompUrl url, final boolean create) throws IOException {
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
            connection.getInputStream().readFrame(context.getTimeout(), TimeUnit.MILLISECONDS);
            this.connections.add(connection);
            return connection;
        }

        return null;
    }

    /**
     * Exchange request and response frames.
     *
     * @param connection connection
     * @param frame request frame
     * @param selector response selector
     * @return frame exchange
     */
    private Future<StompFameExchange> exchange(
            final Connection connection,
            final Frame frame,
            final FrameSelector selector
    ) {
        if (log.isDebugEnabled()) {
            log.debug("Sending " + frame);
        }

        return senderExecutor.submit(new Callable<StompFameExchange>() {
            @Override
            public StompFameExchange call() throws Exception {
                connection.getOutputStream().writeFrame(frame);

                if (selector != null) {
                    final CountDown countDown = new CountDown(context.getTimeout(), TimeUnit.MILLISECONDS);
                    while (countDown.remaining() > 0) {
                        final Iterator<Frame> iterator = connection.incomingQueue.iterator();
                        while (iterator.hasNext()) {
                            final Frame next = iterator.next();
                            if (selector.select(next)) {
                                return new StompFameExchange(frame, next);
                            }
                        }
                        final List<Frame> queue = connection.getIncomingQueue();
                        synchronized (queue) {
                            queue.wait(countDown.remaining());
                        }
                    }
                    return null;
                } else {
                    return new StompFameExchange(frame, new EmptyFrame());
                }

            }
        });
    }

    /**
     * Subscription holder.
     */
    @Data
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Subscription {

        @Getter
        private final String id;

        @Getter
        private final StompFameHandler handler;

    }

    /**
     * Connection holder.
     */
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

        /**
         * Default constructor.
         *
         * @param base base url
         * @param context context
         * @param socket sockert
         * @throws IOException if an I/O error occurs
         */
        Connection(
                final StompUrl base,
                final StompContext context,
                final Socket socket
        ) throws IOException {
            this.base = base;
            this.inputStream = new StompInputStream(context, socket.getInputStream());
            this.outputStream = new StompOutputStream(context, socket.getOutputStream());
        }

    }

    /**
     * Thread factory for named threads.
     */
    private static class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger();
        private final String name;

        /**
         * Default cosntructor.
         *
         * @param name thread family name.
         */
        NamedThreadFactory(final String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, String.format("%s-%s", name, counter.incrementAndGet()));
        }
    }

}
