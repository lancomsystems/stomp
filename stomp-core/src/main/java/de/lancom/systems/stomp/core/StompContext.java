package de.lancom.systems.stomp.core;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import de.lancom.systems.stomp.core.connection.StompConnection;
import de.lancom.systems.stomp.core.connection.StompFrameAwaitJob;
import de.lancom.systems.stomp.core.connection.StompFrameContext;
import de.lancom.systems.stomp.core.connection.StompFrameTransmitJob;
import de.lancom.systems.stomp.core.connection.StompSubscription;
import de.lancom.systems.stomp.core.promise.DeferredFactory;
import de.lancom.systems.stomp.core.promise.Promise;
import de.lancom.systems.stomp.core.promise.callback.ExecutorCallback;
import de.lancom.systems.stomp.core.util.NamedDaemonThreadFactory;
import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompHeader;
import de.lancom.systems.stomp.core.wire.StompVersion;
import de.lancom.systems.stomp.core.wire.frame.AckFrame;
import de.lancom.systems.stomp.core.wire.frame.ConnectFrame;
import de.lancom.systems.stomp.core.wire.frame.ConnectedFrame;
import de.lancom.systems.stomp.core.wire.frame.DisconnectFrame;
import de.lancom.systems.stomp.core.wire.frame.MessageFrame;
import de.lancom.systems.stomp.core.wire.frame.NackFrame;
import de.lancom.systems.stomp.core.wire.frame.ReceiptFrame;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Context for general information and settings used in stomp communication.
 */
@Slf4j
public class StompContext {
    private static final long RECONNECT_TIMEOUT = 500;
    private static final long DEFAULT_TIMEOUT = 10000;

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(
            new NamedDaemonThreadFactory("Stomp")
    );

    private final Map<String, Class<? extends StompFrame>> frameClasses = new HashMap<>();
    private final List<StompConnection> connections = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean();

    @Getter
    private final Selector selector;

    @Getter
    private final DeferredFactory deferred;

    @Getter
    @Setter
    private long timeout = DEFAULT_TIMEOUT;

    @Getter
    @Setter
    private boolean receiptsEnabled = true;

    @Getter
    @Setter
    private List<StompVersion> stompVersions = Arrays.asList(
            StompVersion.VERSION_1_0,
            StompVersion.VERSION_1_1,
            StompVersion.VERSION_1_2
    );

    /**
     * Create a new stomp context and run.
     *
     * @return context
     */
    public static StompContext run() {
        final StompContext context = new StompContext();
        context.start();
        return context;
    }

    /**
     * Default constructor.
     */
    public StompContext() {

        try {
            this.selector = Selector.open();
            this.deferred = new DeferredFactory(EXECUTOR_SERVICE);

            // register client frames
            this.registerFrame(StompAction.CONNECT.value(), ConnectFrame.class);
            this.registerFrame(StompAction.DISCONNECT.value(), DisconnectFrame.class);
            this.registerFrame(StompAction.SEND.value(), SendFrame.class);
            this.registerFrame(StompAction.ACK.value(), AckFrame.class);
            this.registerFrame(StompAction.NACK.value(), NackFrame.class);

            // register server frames
            this.registerFrame(StompAction.CONNECTED.value(), ConnectedFrame.class);
            this.registerFrame(StompAction.RECEIPT.value(), ReceiptFrame.class);
            this.registerFrame(StompAction.MESSAGE.value(), MessageFrame.class);

        } catch (final Exception ex) {
            throw new RuntimeException("Failed to initialize stomp context", ex);
        }

    }

    /**
     * Register frame class for a given action.
     *
     * @param action action
     * @param frameClass frame class
     */
    public void registerFrame(final String action, final Class<? extends StompFrame> frameClass) {
        this.frameClasses.put(action, frameClass);
    }

    /**
     * Create a new frame using the given action.
     *
     * @param action action
     * @return frame
     */
    public StompFrame createFrame(final String action) {
        return createFrame(action, null);
    }

    /**
     * Create a new frame using the given action and headers.
     *
     * @param action action
     * @param headers headers
     * @return frame
     */
    public StompFrame createFrame(final String action, final Map<String, String> headers) {
        final Class<? extends StompFrame> frameClass = frameClasses.get(action);
        final StompFrame frame;

        if (frameClass != null) {
            frame = createFrame(frameClass, headers);
        } else {
            frame = new StompFrame(action);
        }
        return frame;
    }

    /**
     * Create a new frame using the given frame class.
     *
     * @param frameClass frame class
     * @param <T> frame type
     * @return frame
     */
    public <T extends StompFrame> T createFrame(final Class<T> frameClass) {
        return createFrame(frameClass, null);
    }

    /**
     * Create a new frame using the given frame class and headers.
     *
     * @param frameClass frame class
     * @param headers headers
     * @param <T> frame type
     * @return frame
     */
    public <T extends StompFrame> T createFrame(final Class<T> frameClass, final Map<String, String> headers) {
        try {
            return frameClass.newInstance();
        } catch (final Exception ex) {
            throw new RuntimeException("Could not defer frame parse class " + frameClass.getName(), ex);
        }
    }

    /**
     * Get connections.
     *
     * @return connections
     */
    public List<StompConnection> getConnections() {
        return Collections.unmodifiableList(this.connections);
    }

    /**
     * Add a connection.
     *
     * @param connection connection
     */
    public void addConnection(final StompConnection connection) {
        this.connections.add(connection);
    }

    /**
     * Remove a connection.
     *
     * @param connection connection
     */
    public void removeConnection(final StompConnection connection) {
        this.connections.remove(connection);
    }

    /**
     * Start the client and listen for new frames.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            this.getDeferred().defer(new FrameTransmitter());
        }
    }

    /**
     * Stop the client and close all connections.
     */
    public void stop() {
        running.set(false);
    }

    /**
     * Frame receiver job.
     */
    private final class FrameTransmitter implements ExecutorCallback {

        @Override
        public void execute() {
            while (running.get()) {
                try {
                    selector.select(RECONNECT_TIMEOUT);

                    for (final StompConnection connection : connections) {
                        if (!connection.getTransmitJobs().isEmpty()) {
                            if (connection.getState() == StompConnection.State.DISCONNECTED) {
                                connection.connect();
                            } else {
                                registerSubscriptions(connection);
                                writeFrames(connection);
                            }
                        }
                    }

                    final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        final SelectionKey key = iterator.next();
                        try {
                            readFrames((StompConnection) key.attachment());
                        } finally {
                            iterator.remove();
                        }
                    }

                } catch (final Exception ex) {
                    if (log.isWarnEnabled()) {
                        log.warn("Error processing stomp messages", ex);
                    }
                }
            }

            final List<Promise<Void>> promises = new ArrayList<>();
            for (final StompConnection connection : connections) {
                promises.add(connection.disconnect());
            }
            final Iterator<Promise<Void>> iterator = promises.iterator();
            while (iterator.hasNext()) {
                iterator.next().get();
                iterator.remove();
            }
        }

        /**
         * Register subscriptions of given connection if required.
         *
         * @param connection connection
         */
        private void registerSubscriptions(final StompConnection connection) {
            for (final StompSubscription subscription : connection.getSubscriptionsForRegistration()) {
                subscription.subscribe();
            }
        }

        /**
         * Write frames to connection.
         *
         * @param connection connection
         */
        private void writeFrames(final StompConnection connection) {
            if (connection.getSerializer() != null) {
                final Iterator<StompFrameTransmitJob> transmitIterator = connection.getTransmitJobs().iterator();
                while (transmitIterator.hasNext()) {
                    final StompFrameTransmitJob job = transmitIterator.next();
                    final StompFrameContext context = job.getContext();
                    try {
                        if (job.getCondition().getAsBoolean()) {
                            connection.applyInterceptors(context);
                            connection.getSerializer().writeFrame(context.getFrame());
                            transmitIterator.remove();
                            job.getDeferred().resolve(context);
                        }
                    } catch (final Exception ex) {
                        connection.closeConnection();
                        if (log.isErrorEnabled()) {
                            log.error(String.format(
                                    "Failed to write %s to %s, retrying",
                                    context.getFrame(),
                                    connection.toString()
                            ), ex);
                        }
                    }
                }
            }
        }

        /**
         * Read frames from connection.
         *
         * @param connection connection
         */
        private void readFrames(final StompConnection connection) {
            if (connection.getDeserializer() != null) {
                try {
                    while (true) {
                        StompFrame frame = connection.getDeserializer().readFrame();
                        if (frame != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Got " + frame);
                            }

                            final StompFrameContext context = new StompFrameContext(frame);
                            connection.applyInterceptors(context);

                            boolean handled = false;
                            if (!handled) {
                                final StompSubscription subscription;

                                final String subscriptionId = frame.getHeader(StompHeader.SUBSCRIPTION);
                                if (subscriptionId != null) {
                                    subscription = connection.getSubscription(subscriptionId);
                                } else {
                                    subscription = null;
                                }

                                if (subscription != null) {
                                    deferred.defer(() -> {
                                        boolean success = false;
                                        try {
                                            success = subscription.getHandler().handle(context);
                                        } catch (final Exception ex) {
                                            success = false;
                                        }

                                        final String ack = context.getFrame().getHeader(StompHeader.ACK);
                                        if (ack != null) {
                                            if (success) {
                                                subscription.getConnection()
                                                        .transmitFrame(new AckFrame(ack))
                                                        .fail(ex -> {
                                                            if (log.isErrorEnabled()) {
                                                                log.error("Could not send ack frame", ex);
                                                            }
                                                        });
                                            } else {
                                                subscription.getConnection()
                                                        .transmitFrame(new NackFrame(ack))
                                                        .fail(ex -> {
                                                            if (log.isErrorEnabled()) {
                                                                log.error("Could not send nack frame", ex);
                                                            }
                                                        });
                                            }
                                        }
                                    });
                                    handled = true;
                                }
                            }
                            if (!handled) {
                                final Iterator<StompFrameAwaitJob> awaitIterator = connection.getAwaitJobs().iterator();
                                while (!handled && awaitIterator.hasNext()) {
                                    final StompFrameAwaitJob job = awaitIterator.next();
                                    if (job.getHandler().handle(context)) {
                                        awaitIterator.remove();
                                        job.getDeferred().resolve(context);
                                        handled = true;
                                    }
                                }
                            }

                            if (!handled && log.isWarnEnabled()) {
                                log.warn("Frame {} has not been processed", frame);
                            }
                        } else {
                            break;
                        }
                    }
                } catch (final Exception ex) {
                    connection.closeConnection();
                    if (log.isErrorEnabled()) {
                        log.error(String.format(
                                "Failed to read frame from %s",
                                connection.toString()
                        ), ex);
                    }
                }
            }
        }
    }

}
