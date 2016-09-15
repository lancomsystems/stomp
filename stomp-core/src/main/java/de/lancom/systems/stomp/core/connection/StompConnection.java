package de.lancom.systems.stomp.core.connection;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import de.lancom.systems.stomp.core.StompContext;
import de.lancom.systems.stomp.core.promise.Deferred;
import de.lancom.systems.stomp.core.promise.Promise;
import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompDeserializer;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompHeader;
import de.lancom.systems.stomp.core.wire.StompSerializer;
import de.lancom.systems.stomp.core.wire.frame.ClientFrame;
import de.lancom.systems.stomp.core.wire.frame.ConnectFrame;
import de.lancom.systems.stomp.core.wire.frame.DisconnectFrame;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Stomp connection.
 */
@Slf4j
public class StompConnection {

    @Getter
    private final Queue<StompFrameTransmitJob> transmitJobs = new ConcurrentLinkedQueue<>();
    @Getter
    private final Queue<StompFrameAwaitJob> awaitJobs = new ConcurrentLinkedQueue<>();
    @Getter
    private final String host;
    @Getter
    private final int port;
    @Getter
    private final ConnectFrame connectFrame;
    @Getter
    private final DisconnectFrame disconnectFrame;
    @Getter
    private Promise<Void> readyPromise;
    @Getter
    private StompDeserializer deserializer;
    @Getter
    private StompSerializer serializer;

    private final List<StompSubscription> subscriptions = new CopyOnWriteArrayList<>();
    private final List<StompFrameContextInterceptor> interceptors = new CopyOnWriteArrayList<>();

    private final StompContext stompContext;

    private SocketChannel channel;
    private long reconnectLock;

    /**
     * Create a new stomp connection for the given host and port using a new stomp context.
     *
     * @param host host
     * @param port port
     */
    public StompConnection(final String host, final int port) {
        this(new StompContext(), host, port);
        this.stompContext.start();
    }

    /**
     * Create a new stomp connection for the given host and port using the given stomp context.
     *
     * @param stompContext stomp context
     * @param host host
     * @param port port
     */
    public StompConnection(final StompContext stompContext, final String host, final int port) {
        this.stompContext = stompContext;
        this.stompContext.addConnection(this);
        this.host = host;
        this.port = port;
        this.connectFrame = new ConnectFrame();
        this.connectFrame.setAcceptStompVersionList(stompContext.getStompVersions());
        this.disconnectFrame = new DisconnectFrame();
    }

    /**
     * Get connection promise.
     *
     * @return promise
     */
    public Promise<Void> getConnectionPromise() {
        return readyPromise;
    }

    /**
     * Get connection state.
     *
     * @return connection state
     */
    public boolean isConnected() {
        return this.channel != null;
    }

    /**
     * Get ready state.
     *
     * @return ready state
     */
    public boolean isReady() {
        return isConnected() && this.readyPromise != null && this.readyPromise.isSuccess();
    }

    /**
     * Connect to host if required.
     *
     * @return connection promise
     */
    public Promise<Void> connect() {
        boolean connect = true;
        connect = connect && !isConnected();
        connect = connect && this.readyPromise == null || this.readyPromise.isFail();
        connect = connect && System.currentTimeMillis() > this.reconnectLock;

        if (connect) {
            this.reconnectLock = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(2);

            if (log.isDebugEnabled()) {
                log.debug("Connecting to " + this);
            }

            final Promise<Void> promise = stompContext.getDeferred().defer(() -> {
                final InetSocketAddress address = new InetSocketAddress(host, port);
                final SocketChannel createdChannel = SocketChannel.open(address);
                createdChannel.configureBlocking(false);

                this.deserializer = new StompDeserializer(this.stompContext, createdChannel);
                this.serializer = new StompSerializer(this.stompContext, createdChannel);
                this.channel = SocketChannel.open(address);
                this.channel.configureBlocking(false);
                if (log.isDebugEnabled()) {
                    log.debug("Connected to " + this);
                }
                this.transmitFrameAndAwait(
                        new StompFrameContext(this.connectFrame),
                        () -> true,
                        c -> Objects.equals(StompAction.CONNECTED.value(), c.getFrame().getAction())
                ).get();
                if (log.isDebugEnabled()) {
                    log.debug("Ready " + this);
                }
                this.reconnectLock = 0;
            });
            promise.fail((ex) -> {
                if (log.isWarnEnabled()) {
                    log.warn("Connection to " + this + " failed, retrying");
                }
            });

            this.readyPromise = promise.then();

            return this.readyPromise;
        } else {
            return this.stompContext.getDeferred().success();
        }
    }

    /**
     * Disconnect from host.
     *
     * @return disconnection promise
     */
    public Promise<Void> disconnect() {
        if (this.readyPromise != null) {
            if (log.isDebugEnabled()) {
                log.debug("Disconnecting from " + this);
            }

            Promise<Void> promise = this.readyPromise;

            for (final StompSubscription subscription : subscriptions) {
                promise = promise.always(subscription::unsubscribe);
            }

            promise = promise.always(() -> this.transmitFrame(disconnectFrame).always());

            promise = promise.always(() -> {
                this.closeConnection();

                if (log.isDebugEnabled()) {
                    log.debug("Disconnected  from" + this);
                }
            });

            return promise;
        } else {
            return stompContext.getDeferred().success();
        }
    }

    /**
     * Close connection.
     */
    public void closeConnection() {
        try {
            if (this.isReady() && log.isErrorEnabled()) {
                log.error("Lost " + this);
            }
            this.readyPromise = null;
            this.serializer = null;
            this.deserializer = null;
            this.channel.close();
            this.channel = null;

            for (final StompSubscription subscription : subscriptions) {
                subscription.reset();
            }
        } catch (final Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Could not close " + this);
            }
        }
    }

    /**
     * Create a new subscription for the given destination using a random id and the given handler.
     *
     * @param destination destination
     * @param handler handler
     * @return subscription
     */
    public StompSubscription createSubscription(
            @NonNull final String destination,
            @NonNull final StompFrameContextHandler handler
    ) {
        return createSubscription(UUID.randomUUID().toString(), destination, handler);
    }

    /**
     * Create a new subscription for the given destination using the given id and the given handler.
     *
     * @param id id
     * @param destination destination
     * @param handler handler
     * @return subscription
     */
    public StompSubscription createSubscription(
            @NonNull final String id,
            @NonNull final String destination,
            @NonNull final StompFrameContextHandler handler
    ) {
        final StompSubscription subscription = new StompSubscription(stompContext, this, id, destination, handler);
        this.subscriptions.add(subscription);
        return subscription;
    }

    /**
     * Get subscriptions.
     *
     * @return subscriptions
     */
    public List<StompSubscription> getSubscriptions() {
        return Collections.unmodifiableList(subscriptions);
    }

    /**
     * Get subscriptions that need registrations.
     *
     * @return subscriptions
     */
    public List<StompSubscription> getSubscriptionsForRegistration() {
        List<StompSubscription> result = null;
        for (final StompSubscription subscription : subscriptions) {
            if (subscription.getSubscriptionPromise() == null) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(subscription);
            }
        }
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    /**
     * Get subscription with the given id.
     *
     * @param id id
     * @return subscription
     */
    public StompSubscription getSubscription(
            @NonNull final String id
    ) {
        for (final StompSubscription subscription : this.subscriptions) {
            if (Objects.equals(id, subscription.getId())) {
                return subscription;
            }
        }
        return null;
    }

    /**
     * Remove subscription with the given id.
     *
     * @param id id
     * @return removed subscription
     */
    public StompSubscription removeSubscription(
            @NonNull final String id
    ) {
        for (final StompSubscription subscription : this.subscriptions) {
            if (Objects.equals(id, subscription.getId())) {
                subscription.unsubscribe();
                this.subscriptions.remove(subscription);
                return subscription;
            }
        }
        return null;
    }

    /**
     * Add interceptor to send interceptor queue.
     *
     * @param interceptor interceptor
     */
    public void addInterceptor(final StompFrameContextInterceptor interceptor) {
        this.addInterceptor(interceptor, true, null);
    }

    /**
     * Add interceptor to frame interceptor queue before instance of given interceptor class.
     *
     * @param interceptor interceptor
     * @param interceptorClass interceptor class
     */
    public void addInterceptorBefore(
            final StompFrameContextInterceptor interceptor,
            final Class<? extends StompFrameContextInterceptor> interceptorClass
    ) {
        this.addInterceptor(interceptor, true, interceptorClass);
    }

    /**
     * Add interceptor to frame interceptor queue after instance of given interceptor class.
     *
     * @param interceptor interceptor
     * @param interceptorClass interceptor class
     */
    public void addInterceptorAfter(
            final StompFrameContextInterceptor interceptor,
            final Class<? extends StompFrameContextInterceptor> interceptorClass
    ) {
        this.addInterceptor(interceptor, false, interceptorClass);
    }

    /**
     * Add interceptor to interceptor list before or after instance of given interceptor class.
     *
     * @param interceptor interceptor
     * @param before should interceptor be inserted before or after given interceptor class.
     * @param interceptorClass interceptor class
     */
    private void addInterceptor(
            final StompFrameContextInterceptor interceptor,
            final boolean before,
            final Class<? extends StompFrameContextInterceptor> interceptorClass
    ) {
        if (!interceptors.isEmpty() && interceptorClass != null) {
            if (before) {
                for (int index = 0; index < interceptors.size(); index++) {
                    if (interceptorClass.isAssignableFrom(interceptors.get(index).getClass())) {
                        interceptors.add(index, interceptor);
                        return;
                    }
                }
            } else {
                for (int index = interceptors.size() - 1; index >= 0; index--) {
                    if (interceptorClass.isAssignableFrom(interceptors.get(index).getClass())) {
                        interceptors.add(index, interceptor);
                        return;
                    }
                }
            }
        }
        if (before && !this.interceptors.isEmpty()) {
            this.interceptors.add(0, interceptor);
        } else {
            this.interceptors.add(interceptor);
        }
    }

    /**
     * Remove interceptor instance from frame interceptor queue.
     *
     * @param interceptor interceptor
     */
    public void removeIntercetor(final StompFrameContextHandler interceptor) {
        final Iterator<StompFrameContextInterceptor> iterator = interceptors.iterator();
        while (iterator.hasNext()) {
            if (Objects.equals(iterator.next(), interceptor)) {
                iterator.remove();
            }
        }
    }

    /**
     * Remove interceptor of given type from frame interceptor queue.
     *
     * @param interceptorClass interceptor class
     */
    public void removeIntercetor(final Class<? extends StompFrameContextHandler> interceptorClass) {
        final Iterator<StompFrameContextInterceptor> iterator = interceptors.iterator();
        while (iterator.hasNext()) {
            if (interceptorClass.isAssignableFrom(iterator.next().getClass())) {
                iterator.remove();
            }
        }
    }

    /**
     * Send the given body to the given destination.
     *
     * @param destination destination
     * @param body body
     * @return promise
     */
    public Promise<StompFrameContext> send(
            final String destination,
            final String body
    ) {
        return transmitFrame(new SendFrame(destination, body));
    }

    /**
     * Send the given body to the given destination.
     *
     * @param destination destination
     * @param body body
     * @return promise
     */
    public Promise<StompFrameContext> send(
            final String destination,
            final byte[] body
    ) {
        return transmitFrame(new SendFrame(destination, body));
    }

    /**
     * Transmit frame.
     *
     * @param frame request frame
     * @return promise
     */
    public Promise<StompFrameContext> transmitFrame(@NonNull final StompFrame frame) {
        return this.transmitFrame(frame, this::isReady);
    }

    /**
     * Transmit frame.
     *
     * @param frame frame
     * @param condition transmit condition
     * @return promise
     */
    public Promise<StompFrameContext> transmitFrame(
            @NonNull final StompFrame frame,
            final BooleanSupplier condition
    ) {
        final StompFrameContext context = new StompFrameContext();
        context.setFrame(frame);
        return this.transmitFrame(context, condition);
    }

    /**
     * Transmit frame.
     *
     * @param context frame context
     * @param condition transmit condition
     * @return promise
     */
    public Promise<StompFrameContext> transmitFrame(
            @NonNull final StompFrameContext context,
            final BooleanSupplier condition
    ) {
        boolean addReceipt = true;
        addReceipt = addReceipt && stompContext.isReceiptsEnabled();
        addReceipt = addReceipt && !context.getFrame().hasHeader(StompHeader.RECEIPT);
        addReceipt = addReceipt && context.getFrame() instanceof ClientFrame;

        if (addReceipt) {
            final String receipt = UUID.randomUUID().toString();
            context.getFrame().setHeader(StompHeader.RECEIPT, receipt);
        }

        final StompFrameContextHandler handler;
        if (context.getFrame().hasHeader(StompHeader.RECEIPT)) {
            handler = c -> Objects.equals(
                    context.getFrame().getHeader(StompHeader.RECEIPT),
                    c.getFrame().getHeader(StompHeader.RECEIPT_ID)
            );
        } else {
            handler = null;
        }

        return this.transmitFrameAndAwait(context, condition, handler);
    }

    /**
     * Transmit frame and await response.
     *
     * @param context frame context
     * @param handler frame handler
     * @return promise
     */
    public Promise<StompFrameContext> transmitFrameAndAwait(
            @NonNull final StompFrameContext context,
            @NonNull final StompFrameContextHandler handler
    ) {
        return this.transmitFrameAndAwait(context, this::isReady, handler);
    }

    /**
     * Transmit frame and await response.
     *
     * @param context frame context
     * @param handler frame handler
     * @param condition transmit condition
     * @return promise
     */
    public Promise<StompFrameContext> transmitFrameAndAwait(
            @NonNull final StompFrameContext context,
            @NonNull final BooleanSupplier condition,
            final StompFrameContextHandler handler
    ) {
        final Deferred<StompFrameContext> result = stompContext.getDeferred().defer();
        try {
            if (handler != null) {
                final Deferred<StompFrameContext> deferred = stompContext.getDeferred().defer();

                awaitFrame(handler).apply(result);

                this.transmitJobs.add(new StompFrameTransmitJob(context, condition, deferred));
            } else {
                this.transmitJobs.add(new StompFrameTransmitJob(context, condition, result));
            }
        } catch (final Exception ex) {
            result.reject(ex);
        }
        return result.getPromise();
    }

    /**
     * Await frame.
     *
     * @param handler frame handler
     * @return promise
     */
    public Promise<StompFrameContext> awaitFrame(
            @NonNull final StompFrameContextHandler handler
    ) {
        final Deferred<StompFrameContext> deferred = stompContext.getDeferred().defer();
        try {
            this.awaitJobs.add(new StompFrameAwaitJob(handler, deferred));
        } catch (final Exception ex) {
            deferred.reject(ex);
        }
        return deferred.getPromise();
    }

    /**
     * Apply interceptors to the given frame context.
     *
     * @param context frame context
     */
    public void applyInterceptors(final StompFrameContext context) {
        if (context != null && !interceptors.isEmpty()) {
            for (final StompFrameContextInterceptor interceptor : interceptors) {
                try {
                    interceptor.intercept(context);
                } catch (final Exception ex) {
                    continue;
                }
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getSimpleName());
        builder.append('(');
        builder.append("host: ").append(host);
        builder.append(", port: ").append(port);
        if (this.getConnectFrame().getLogin() != null) {
            builder.append(", login: ").append(this.getConnectFrame().getLogin());
        }
        builder.append(')');
        return builder.toString();
    }
}
