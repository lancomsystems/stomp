package de.lancom.systems.stomp.core.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import de.lancom.systems.stomp.core.StompContext;
import de.lancom.systems.stomp.core.connection.StompConnection;
import de.lancom.systems.stomp.core.connection.StompFrameContext;
import de.lancom.systems.stomp.core.connection.StompFrameContextHandler;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptor;
import de.lancom.systems.stomp.core.connection.StompSubscription;
import de.lancom.systems.stomp.core.promise.Promise;
import de.lancom.systems.stomp.core.wire.StompFrame;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Stomp client.
 * This client allows several concurrent connectionHolders to different servers using stomp urls.
 */
@Slf4j
public class StompClient {

    @Getter
    private final StompContext stompContext;

    private final List<StompFrameContextInterceptor> interceptors = new CopyOnWriteArrayList<>();

    private final Map<StompUrl, StompConnection> connectionMap = new HashMap<>();

    private final StompFrameContextInterceptor interceptorDelegator;

    /**
     * Create client.
     */
    public StompClient() {
        this(new StompContext());
        this.stompContext.start();
    }

    /**
     * Create client with given stomp context.
     *
     * @param stompContext stomp context
     */
    public StompClient(final StompContext stompContext) {
        this.stompContext = stompContext;
        this.interceptorDelegator = c -> {
            for (final StompFrameContextInterceptor interceptor : interceptors) {
                interceptor.intercept(c);
            }
        };
    }

    /**
     * Get connection for the given url.
     *
     * @param url url
     * @return connection
     */
    public StompConnection getConnection(final StompUrl url) {
        return getConnection(url, false);
    }

    /**
     * Get or create connection for the given url.
     *
     * @param url url
     * @param create create connection if it does not exist
     * @return connection
     */
    private StompConnection getConnection(final StompUrl url, final boolean create) {
        final StompUrl base = url.getBase();
        StompConnection connection = connectionMap.get(base);
        if (connection == null && create) {
            connection = new StompConnection(url.getHost(), url.getPort());
            connection.getConnectFrame().setLogin(url.getLogin());
            connection.getConnectFrame().setPasscode(url.getPasscode());
            connection.addInterceptor(this.interceptorDelegator);

            connectionMap.put(base, connection);
        }
        return connection;
    }

    /**
     * Send the given body.
     *
     * @param url url
     * @param body body
     * @return promise
     */
    public Promise<StompFrameContext> send(
            final StompUrl url,
            final String body
    ) {
        return getConnection(url, true).send(url.getDestination(), body);
    }

    /**
     * Send the given body.
     *
     * @param url url
     * @param body body
     * @return promise
     */
    public Promise<StompFrameContext> send(
            final StompUrl url,
            final byte[] body
    ) {
        return getConnection(url, true).send(url.getDestination(), body);
    }

    /**
     * Send the given send frame to the given url and wait for response if expected.
     *
     * @param url url
     * @param frame frame
     * @return promise
     * @throws IOException if an I/O error occurs
     */
    public Promise<StompFrameContext> transmitFrame(
            final StompUrl url,
            final StompFrame frame
    ) throws IOException {
        return getConnection(url, true).transmitFrame(frame);
    }

    /**
     * Subscribe to the given url using the given id and frame callback.
     *
     * @param url url
     * @param handler frame callback
     * @return promise
     */
    public StompSubscription createSubscription(
            final StompUrl url,
            final StompFrameContextHandler handler
    ) {
        return getConnection(url, true).createSubscription(url.getDestination(), handler);
    }

    /**
     * Subscribe to the given url using the given subscribe frame and frame callback.
     *
     * @param id id
     * @param url url
     * @param handler frame callback
     * @return promise
     */
    public StompSubscription createSubscription(
            final StompUrl url,
            final String id,
            final StompFrameContextHandler handler
    ) {
        return getConnection(url, true).createSubscription(id, url.getDestination(), handler);
    }

    /**
     * Unsubscribe from the given url using the given id.
     *
     * @param url url
     * @param id id
     * @return promise
     */
    public StompSubscription removeSubscription(
            final StompUrl url,
            final String id
    ) {
        return this.getConnection(url, true).removeSubscription(id);
    }

    /**
     * Add interceptorDelegator to send interceptorDelegator queue.
     *
     * @param interceptor interceptorDelegator
     */
    public void addInterceptor(final StompFrameContextInterceptor interceptor) {
        this.addInterceptor(interceptor, true, null);
    }

    /**
     * Add interceptorDelegator to frame interceptorDelegator queue before instance of given interceptorDelegator class.
     *
     * @param interceptor interceptorDelegator
     * @param interceptorClass interceptorDelegator class
     */
    public void addInterceptorBefore(
            final StompFrameContextInterceptor interceptor,
            final Class<? extends StompFrameContextInterceptor> interceptorClass
    ) {
        this.addInterceptor(interceptor, true, interceptorClass);
    }

    /**
     * Add interceptorDelegator to frame interceptorDelegator queue after instance of given interceptorDelegator class.
     *
     * @param interceptor interceptorDelegator
     * @param interceptorClass interceptorDelegator class
     */
    public void addInterceptorAfter(
            final StompFrameContextInterceptor interceptor,
            final Class<? extends StompFrameContextInterceptor> interceptorClass
    ) {
        this.addInterceptor(interceptor, false, interceptorClass);
    }

    /**
     * Add interceptorDelegator to interceptorDelegator list before or after instance of given interceptorDelegator
     * class.
     *
     * @param interceptor interceptorDelegator
     * @param before should interceptorDelegator be inserted before or after given interceptorDelegator class.
     * @param interceptorClass interceptorDelegator class
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
     * Remove interceptorDelegator instance from frame interceptorDelegator queue.
     *
     * @param target target
     */
    public void removeIntercetor(final StompFrameContextInterceptor target) {
        for (final StompFrameContextInterceptor interceptor : interceptors) {
            if (Objects.equals(interceptor, target)) {
                interceptors.remove(interceptor);
            }
        }
    }

    /**
     * Remove interceptorDelegator of given type from frame interceptorDelegator queue.
     *
     * @param interceptorClass interceptorDelegator class
     */
    public void removeIntercetor(final Class<? extends StompFrameContextInterceptor> interceptorClass) {
        for (final StompFrameContextInterceptor interceptor : interceptors) {
            if (interceptorClass.isAssignableFrom(interceptor.getClass())) {
                interceptors.remove(interceptor);
            }
        }
    }

    /**
     * Await frame.
     *
     * @param url url
     * @param handler frame handler
     * @return promise
     */
    public Promise<StompFrameContext> awaitFrame(
            @NonNull final StompUrl url,
            @NonNull final StompFrameContextHandler handler
    ) {
        return getConnection(url, true).awaitFrame(handler);
    }

    /**
     * Close client and all connections.
     */
    public void close() {
        for (final StompConnection connection : connectionMap.values()) {
            connection.close();
        }
    }
}
