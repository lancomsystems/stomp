package de.lancom.systems.stomp.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.lancom.systems.stomp.core.util.CountDown;
import de.lancom.systems.stomp.core.util.NamedDaemonThreadFactory;
import de.lancom.systems.stomp.core.wire.StompContext;
import de.lancom.systems.stomp.core.wire.StompDeserializer;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompFrameHandler;
import de.lancom.systems.stomp.core.wire.StompInterceptor;
import de.lancom.systems.stomp.core.wire.StompHeader;
import de.lancom.systems.stomp.core.wire.StompSerializer;
import de.lancom.systems.stomp.core.wire.StompUrl;
import de.lancom.systems.stomp.core.wire.frame.AckFrame;
import de.lancom.systems.stomp.core.wire.frame.ConnectFrame;
import de.lancom.systems.stomp.core.wire.frame.ConnectedFrame;
import de.lancom.systems.stomp.core.wire.frame.DisconnectFrame;
import de.lancom.systems.stomp.core.wire.frame.NackFrame;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import de.lancom.systems.stomp.core.wire.frame.SubscribeFrame;
import de.lancom.systems.stomp.core.wire.frame.UnsubscribeFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Stomp client.
 * This client allows several concurrent connectionHolders to different servers using stomp urls.
 */
@Slf4j
public class StompClient {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final ExecutorService exchangeExecutor = Executors.newSingleThreadExecutor(createThreadFactory("Exchange"));
    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(createThreadFactory("Consumer"));

    private final List<ConnectionHolder> connectionHolders = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Getter
    private final StompContext context = new StompContext();

    private final List<FrameInterceptorHolder> interceptorHolders = new ArrayList<>();

    /**
     * Default constructor.
     */
    public StompClient() {
        COUNTER.incrementAndGet();
    }

    /**
     * Start the client and listen for new frames.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            this.exchangeExecutor.submit(new FrameTransmitter());
        }
    }

    /**
     * Stop the client and close all connections.
     */
    public void stop() {
        if (running.get()) {
            for (final ConnectionHolder connectionHolder : connectionHolders) {

                for (final SubscriptionHolder subscriptionHolder : connectionHolder.getSubscriptionMap().values()) {
                    try {
                        this.unsubscribe(connectionHolder.getBase(), subscriptionHolder.getId());
                    } catch (final Exception ex) {
                        if (log.isWarnEnabled()) {
                            log.warn("Could not unsubscribe " + subscriptionHolder.getId(), ex);
                        }
                    }

                }

                try {
                    final DisconnectFrame disconnectFrame = context.createFrame(DisconnectFrame.class);
                    this.transmitFrame(connectionHolder.getBase(), disconnectFrame);
                } catch (final IOException ex) {
                    if (log.isWarnEnabled()) {
                        log.warn("Could not disconnect from " + connectionHolder.getBase(), ex);
                    }
                }
            }

            final CountDown countDown = new CountDown(1, TimeUnit.MINUTES);

            try {
                this.running.set(false);
                this.exchangeExecutor.shutdown();
                this.exchangeExecutor.awaitTermination(countDown.remaining(), TimeUnit.MILLISECONDS);
            } catch (final InterruptedException ex) {
                if (log.isWarnEnabled()) {
                    log.warn("Failed to shut down receiver", ex);
                }
            }

            try {
                this.consumerExecutor.shutdown();
                this.consumerExecutor.awaitTermination(countDown.remaining(), TimeUnit.MILLISECONDS);
            } catch (final InterruptedException ex) {
                if (log.isWarnEnabled()) {
                    log.warn("Failed to shut down sender", ex);
                }
            }

            for (final ConnectionHolder connectionHolder : connectionHolders) {
                try {
                    connectionHolder.getChannel().close();
                } catch (final Exception ex) {
                    if (log.isWarnEnabled()) {
                        log.warn("Could not close connectionHolder to " + connectionHolder.getBase(), ex);
                    }
                }
            }
        }
    }

    /**
     * Add interceptor to send interceptor queue.
     *
     * @param interceptor interceptor
     * @param actions frame actions this interceptor is applied to
     */
    public void addInterceptor(final StompInterceptor interceptor, final String... actions) {
        this.addInterceptor(interceptor, true, null, actions);
    }

    /**
     * Add interceptor to frame interceptor queue before instance of given interceptor class.
     *
     * @param interceptor interceptor
     * @param interceptorClass interceptor class
     * @param actions frame actions this interceptor is applied to
     */
    public void addInterceptorBefore(
            final StompInterceptor interceptor,
            final Class<? extends StompInterceptor> interceptorClass,
            final String... actions
    ) {
        this.addInterceptor(interceptor, true, interceptorClass, actions);
    }

    /**
     * Add interceptor to frame interceptor queue after instance of given interceptor class.
     *
     * @param interceptor interceptor
     * @param interceptorClass interceptor class
     * @param actions frame actions this interceptor is applied to
     */
    public void addInterceptorAfter(
            final StompInterceptor interceptor,
            final Class<? extends StompInterceptor> interceptorClass,
            final String... actions
    ) {
        this.addInterceptor(interceptor, false, interceptorClass, actions);
    }

    /**
     * Remove interceptor instance from frame interceptor queue.
     *
     * @param interceptor interceptor
     */
    public void removeIntercetor(final StompInterceptor interceptor) {
        final Iterator<FrameInterceptorHolder> iterator = interceptorHolders.iterator();
        while (iterator.hasNext()) {
            if (Objects.equals(iterator.next().getInterceptor(), interceptor)) {
                iterator.remove();
            }
        }
    }

    /**
     * Remove interceptor of given type from frame interceptor queue.
     *
     * @param interceptorClass interceptor class
     */
    public void removeIntercetor(final Class<? extends StompInterceptor> interceptorClass) {
        final Iterator<FrameInterceptorHolder> iterator = interceptorHolders.iterator();
        while (iterator.hasNext()) {
            if (interceptorClass.isAssignableFrom(iterator.next().getClass())) {
                iterator.remove();
            }
        }
    }

    /**
     * Send the given message body to the given url and wait for response if expected.
     *
     * @param url url
     * @param body body
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> send(
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
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> send(
            final StompUrl url,
            final String body,
            final Map<String, String> headers
    ) throws IOException {
        final SendFrame frame = this.context.createFrame(SendFrame.class, headers);
        frame.setDestination(url.getDestination());
        frame.setBodyAsString(body);
        return this.send(url, frame);
    }

    /**
     * Send the given send frame to the given url and wait for response if expected.
     *
     * @param url url
     * @param sendFrame frame
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> send(
            final StompUrl url,
            final SendFrame sendFrame
    ) throws IOException {
        return this.transmitFrameAwaitingReceipt(url, sendFrame);
    }

    /**
     * Subscribe to the given url using the given id and frame handler.
     *
     * @param url url
     * @param id id
     * @param handler frame handler
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> subscribe(
            final StompUrl url,
            final String id,
            final StompFrameHandler handler
    ) throws IOException {

        final SubscribeFrame frame = this.context.createFrame(SubscribeFrame.class);
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
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> subscribe(
            final StompUrl url,
            final SubscribeFrame subscribeFrame,
            final StompFrameHandler handler
    ) throws IOException {
        final ConnectionHolder connectionHolder = this.getConnection(url, true);
        connectionHolder.getSubscriptionMap().put(
                subscribeFrame.getId(), new SubscriptionHolder(url, subscribeFrame.getId(), handler)
        );

        return this.transmitFrameAwaitingReceipt(url, subscribeFrame);
    }

    /**
     * Unsubscribe from the given url using the given id.
     *
     * @param url url
     * @param id id
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> unsubscribe(
            final StompUrl url,
            final String id
    ) throws IOException {

        final ConnectionHolder connectionHolder = this.getConnection(url, true);
        final SubscriptionHolder subscriptionHolder = connectionHolder.getSubscriptionMap().get(id);
        if (subscriptionHolder != null) {
            final UnsubscribeFrame frame = this.context.createFrame(UnsubscribeFrame.class);
            frame.setId(id);

            return unsubscribe(url, frame);
        }
        return null;
    }

    /**
     * Unsubscribe from the given url using the given unsubscribe frame.
     *
     * @param url url
     * @param unsubscribeFrame unsubscribe frame
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> unsubscribe(
            final StompUrl url,
            final UnsubscribeFrame unsubscribeFrame
    ) throws IOException {
        return this.transmitFrameAwaitingReceipt(url, unsubscribeFrame);
    }

    /**
     * Send ack for the given message frame.
     *
     * @param url url
     * @param frame frame
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> ack(final StompUrl url, final StompFrame frame) throws IOException {
        return ack(url, new AckFrame(frame));
    }

    /**
     * Send ack using the given ack frame.
     *
     * @param url url
     * @param ackFrame ack frame
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> ack(final StompUrl url, final AckFrame ackFrame) throws IOException {
        return this.transmitFrameAwaitingReceipt(url, ackFrame);
    }

    /**
     * Send nack for the given message frame.
     *
     * @param url url
     * @param frame frame
     * @return frame send
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> nack(
            final StompUrl url,
            final StompFrame frame
    ) throws IOException {
        return nack(url, new NackFrame(frame));
    }

    /**
     * Send nack using the given ack frame.
     *
     * @param url url
     * @param nackFrame nack frame
     * @return frame send
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> nack(
            final StompUrl url,
            final NackFrame nackFrame
    ) throws IOException {
        return this.transmitFrameAwaitingReceipt(url, nackFrame);
    }

    /**
     * Add interceptor to frame interceptor queue before or after instance of given interceptor class.
     *
     * @param interceptor interceptor
     * @param before should interceptor be inserted before or after given interceptor class.
     * @param interceptorClass interceptor class
     * @param actions frame actions this interceptor is applied to
     */
    private void addInterceptor(
            final StompInterceptor interceptor,
            final boolean before,
            final Class<? extends StompInterceptor> interceptorClass,
            final String... actions
    ) {
        final FrameInterceptorHolder interceptorHolder = new FrameInterceptorHolder(
                interceptor,
                Arrays.asList(actions)
        );
        if (!interceptorHolders.isEmpty() && interceptorClass != null) {
            if (before) {
                for (int index = 0; index < interceptorHolders.size(); index++) {
                    if (interceptorClass.isAssignableFrom(interceptorHolders.get(index).getClass())) {
                        interceptorHolders.add(index, interceptorHolder);
                        return;
                    }
                }
            } else {
                for (int index = interceptorHolders.size() - 1; index >= 0; index--) {
                    if (interceptorClass.isAssignableFrom(interceptorHolders.get(index).getClass())) {
                        interceptorHolders.add(index, interceptorHolder);
                        return;
                    }
                }
            }
        }
        if (before && !this.interceptorHolders.isEmpty()) {
            this.interceptorHolders.add(0, interceptorHolder);
        } else {
            this.interceptorHolders.add(interceptorHolder);
        }
    }

    /**
     * Find connection for the given url or create a new one.
     *
     * @param url url
     * @param create creation flag
     * @return connection or null if none exists and should not be created
     * @throws IOException if an I/O error occurs
     */
    private ConnectionHolder getConnection(final StompUrl url, final boolean create) throws IOException {
        final StompUrl base = url.getBase();

        for (final ConnectionHolder connectionHolder : connectionHolders) {
            if (Objects.equals(base, connectionHolder.getBase())) {
                return connectionHolder;
            }
        }
        if (create) {
            final SocketChannel channel = SocketChannel.open(new InetSocketAddress(url.getHost(), url.getPort()));
            channel.configureBlocking(false);
            final ConnectionHolder connectionHolder = new ConnectionHolder(base, context, channel);
            try {
                this.connectionHolders.add(connectionHolder);
                final ConnectFrame connectFrame = this.context.createFrame(ConnectFrame.class);
                if (context.getStompVersion() != null) {
                    connectFrame.setAcceptVersion(context.getStompVersion().value());
                }
                this.transmitFrameAwaitingFrame(url, connectFrame, (u, f) -> {
                    return f instanceof ConnectedFrame;
                }).get(context.getTimeout(), TimeUnit.MILLISECONDS);
            } catch (final Exception ex) {
                this.connectionHolders.remove(connectionHolder);
                throw new RuntimeException(String.format("Could not connect to %s", url), ex);
            }
            return connectionHolder;
        }

        return null;
    }

    /**
     * Transmit frame and wait for receipt if required.
     *
     * @param url url
     * @param frame frame
     * @return future
     * @throws IOException if an I/O error occur
     */
    private CompletableFuture<Boolean> transmitFrameAwaitingReceipt(
            final StompUrl url, final StompFrame frame
    ) throws IOException {
        final StompFrameHandler handler;

        boolean addReceipt = true;
        addReceipt = addReceipt && context.isReceiptsEnabled();
        addReceipt = addReceipt && !frame.hasHeader(StompHeader.RECEIPT);
        addReceipt = addReceipt && !ConnectFrame.class.isAssignableFrom(frame.getClass());

        if (addReceipt) {
            final String receipt = UUID.randomUUID().toString();
            frame.setHeader(StompHeader.RECEIPT, receipt);
        }

        if (frame.hasHeader(StompHeader.RECEIPT)) {
            handler = (u, f) -> Objects.equals(
                    frame.getHeader(StompHeader.RECEIPT),
                    f.getHeader(StompHeader.RECEIPT_ID)
            );
        } else {
            handler = null;
        }

        return transmitFrameAwaitingFrame(url, frame, handler);
    }

    /**
     * Transmit frame and await frame if required.
     *
     * @param url url
     * @param frame frame
     * @param handler frame handler
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> transmitFrameAwaitingFrame(
            @NonNull final StompUrl url,
            @NonNull final StompFrame frame,
            final StompFrameHandler handler
    ) throws IOException {

        final CompletableFuture<Boolean> future;
        if (handler != null) {
            future = awaitFrame(url, handler);
            transmitFrame(url, frame);
        } else {
            future = transmitFrame(url, frame);
        }

        return future;
    }

    /**
     * Transmit frame.
     *
     * @param url url
     * @param frame request frame
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> transmitFrame(
            @NonNull final StompUrl url,
            @NonNull final StompFrame frame
    ) throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("Sending " + frame);
        }

        final StompUrl targetUrl;
        if (frame.hasHeader(StompHeader.DESTINATION)) {
            targetUrl = url.withDestination(frame.getHeader(StompHeader.DESTINATION));
        } else {
            targetUrl = url;
        }

        final ConnectionHolder connectionHolder = getConnection(url, true);
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        connectionHolder.getOutgoingQueue().add(new FrameSenderHolder(targetUrl, frame, future));
        return future;
    }

    /**
     * Await frame.
     *
     * @param url url
     * @param handler frame handler
     * @return future
     * @throws IOException if an I/O error occurs
     */
    public CompletableFuture<Boolean> awaitFrame(
            @NonNull final StompUrl url,
            @NonNull final StompFrameHandler handler
    ) throws IOException {
        final ConnectionHolder connectionHolder = getConnection(url, true);
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        connectionHolder.getIncomingQueue().add(new FrameHandlerHolder(url, handler, future));
        return future;
    }

    /**
     * Apply interceptors to the given frame.
     *
     * @param url url
     * @param frame frame
     * @return processed frame
     * @throws Exception if interceptor handling failes
     */
    private StompFrame applyFrameInterceptors(final StompUrl url, final StompFrame frame) throws Exception {
        StompFrame result = frame;
        if (!interceptorHolders.isEmpty()) {
            for (final FrameInterceptorHolder interceptorHolder : interceptorHolders) {
                if (result == null) {
                    return null;
                }
                boolean apply = false;
                apply = apply || interceptorHolder.getActions().isEmpty();
                apply = apply || interceptorHolder.getActions().contains(frame.getAction());

                if (apply) {
                    result = interceptorHolder.getInterceptor().intercept(url, frame);
                }
            }
        }
        return result;
    }

    /**
     * Create a new named thread factory for this client.
     *
     * @param name name
     * @return thread factory
     */
    private ThreadFactory createThreadFactory(final String name) {
        return new NamedDaemonThreadFactory(String.format("StompClient %s %s", COUNTER.get(), name));
    }

    /**
     * Frame receiver job.
     */
    private final class FrameTransmitter implements Runnable {
        @Override
        public void run() {
            if (log.isDebugEnabled()) {
                log.debug("Stomp client transmitter started");
            }
            while (running.get()) {
                for (final ConnectionHolder connectionHolder : connectionHolders) {
                    if (connectionHolder.getChannel().isConnected()) {
                        writeFrames(connectionHolder);
                        readFrames(connectionHolder);
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Stomp client transmitter stopped");
            }
        }

        /**
         * Write frames to connection.
         *
         * @param connectionHolder connection holder
         */
        private void writeFrames(final ConnectionHolder connectionHolder) {
            final Iterator<FrameSenderHolder> outgoingIterator = connectionHolder.getOutgoingQueue().iterator();
            while (outgoingIterator.hasNext()) {
                try {
                    final FrameSenderHolder holder = outgoingIterator.next();
                    StompFrame frame = holder.getFrame();

                    frame = applyFrameInterceptors(holder.getUrl(), frame);

                    if (frame != null) {
                        connectionHolder.getSerializer().writeFrame(frame);
                    }
                    outgoingIterator.remove();
                    holder.getFuture().complete(true);
                } catch (final Exception ex) {
                    if (log.isErrorEnabled()) {
                        log.error("Failed to write messages to " + connectionHolder.getBase(), ex);
                    }
                }
            }
        }

        /**
         * Read frames from connection.
         *
         * @param connectionHolder connection holder
         */
        private void readFrames(final ConnectionHolder connectionHolder) {
            while (true) {
                try {
                    StompFrame frame = connectionHolder.getDeserializer().readFrame();
                    if (frame != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Got " + frame);
                        }

                        frame = applyFrameInterceptors(connectionHolder.getBase().withDestination(
                                frame.getHeader(StompHeader.DESTINATION)
                        ), frame);

                        boolean handled = false;
                        if (!handled) {
                            final String subscriptionId = frame.getHeader(StompHeader.SUBSCRIPTION);
                            if (subscriptionId != null) {
                                final SubscriptionHolder subscriptionHolder = connectionHolder
                                        .getSubscriptionMap()
                                        .get(subscriptionId);

                                if (subscriptionHolder != null) {
                                    StompClient.this.consumerExecutor.execute(new FrameConsumer(
                                            connectionHolder,
                                            subscriptionHolder,
                                            frame
                                    ));
                                    handled = true;
                                }
                            }
                        }

                        if (!handled) {
                            final Iterator<FrameHandlerHolder> iterator = connectionHolder.getIncomingQueue()
                                    .iterator();
                            while (iterator.hasNext()) {
                                final FrameHandlerHolder holder = iterator.next();
                                if (holder.getHandler().handle(connectionHolder.getBase(), frame)) {
                                    holder.getFuture().complete(true);
                                }
                                handled = true;
                            }
                        }

                        if (!handled && log.isWarnEnabled()) {
                            log.warn("Frame {} has not been processed", frame);
                        }
                    } else {
                        break;
                    }
                } catch (final Exception ex) {
                    if (log.isErrorEnabled()) {
                        log.error("Failed to read messages from " + connectionHolder.getBase(), ex);
                    }
                }
            }
        }
    }

    /**
     * Frame consumer job.
     */
    private final class FrameConsumer implements Runnable {
        private final ConnectionHolder connectionHolder;
        private final SubscriptionHolder subscriptionHolder;
        private final StompFrame frame;

        /**
         * Default constructor.
         *
         * @param connectionHolder connection holder
         * @param subscriptionHolder subscription holder
         * @param frame frame
         */
        private FrameConsumer(
                final ConnectionHolder connectionHolder,
                final SubscriptionHolder subscriptionHolder,
                final StompFrame frame
        ) {
            this.connectionHolder = connectionHolder;
            this.subscriptionHolder = subscriptionHolder;
            this.frame = frame;
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                success = subscriptionHolder.getHandler().handle(subscriptionHolder.getUrl(), frame);
            } catch (final Exception ex) {
                if (log.isErrorEnabled()) {
                    log.error("Failed to process " + frame, ex);
                }
                success = false;
            }

            final String ack = frame.getHeader(StompHeader.ACK);
            if (ack != null) {
                if (success) {
                    try {
                        StompClient.this.ack(
                                subscriptionHolder.getUrl(), frame
                        ).get(context.getTimeout(), TimeUnit.MILLISECONDS);
                    } catch (final Exception ex) {
                        if (log.isErrorEnabled()) {
                            log.error("Failed to nack " + frame, ex);
                        }
                    }
                } else {
                    try {
                        StompClient.this.nack(
                                subscriptionHolder.getUrl(), frame
                        ).get(context.getTimeout(), TimeUnit.MILLISECONDS);
                    } catch (final Exception ex) {
                        if (log.isErrorEnabled()) {
                            log.error("Failed to nack " + frame, ex);
                        }
                    }
                }
            }
        }
    }

    /**
     * SubscriptionHolder holder.
     */
    @Data
    @AllArgsConstructor
    private static class SubscriptionHolder {

        @NonNull
        private final StompUrl url;

        @NonNull
        private final String id;

        @NonNull
        private final StompFrameHandler handler;

    }

    /**
     * ConnectionHolder holder.
     */
    @Data
    private static class ConnectionHolder {

        private final StompUrl base;
        private final SocketChannel channel;
        private final StompDeserializer deserializer;
        private final StompSerializer serializer;
        private final Map<String, SubscriptionHolder> subscriptionMap = new ConcurrentSkipListMap<>();

        private Queue<FrameSenderHolder> outgoingQueue = new ConcurrentLinkedQueue<>();
        private Queue<FrameHandlerHolder> incomingQueue = new ConcurrentLinkedQueue<>();

        /**
         * Default constructor.
         *
         * @param base base url
         * @param context context
         * @param channel channel channel
         * @throws IOException if an I/O error occurs
         */
        ConnectionHolder(
                @NonNull final StompUrl base,
                @NonNull final StompContext context,
                @NonNull final SocketChannel channel
        ) throws IOException {
            this.base = base;
            this.channel = channel;
            this.deserializer = new StompDeserializer(context, channel);
            this.serializer = new StompSerializer(context, channel);
        }

    }

    /**
     * Holder for messages to send.
     */
    @Data
    private static class FrameSenderHolder {
        private final StompUrl url;
        private final StompFrame frame;
        private final CompletableFuture<Boolean> future;
    }

    /**
     * Holder for messages to consume.
     */
    @Data
    private static class FrameHandlerHolder {
        private final StompUrl url;
        private final StompFrameHandler handler;
        private final CompletableFuture<Boolean> future;
    }

    /**
     * StompFrame interceptor holder.
     */
    @Data
    private static class FrameInterceptorHolder {

        @NonNull
        private final StompInterceptor interceptor;

        @NonNull
        private final List<String> actions;

    }

}
