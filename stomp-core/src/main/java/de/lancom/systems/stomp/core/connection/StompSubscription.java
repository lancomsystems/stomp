package de.lancom.systems.stomp.core.connection;

import de.lancom.systems.defer.Deferred;
import de.lancom.systems.defer.Promise;
import de.lancom.systems.stomp.core.StompContext;
import de.lancom.systems.stomp.core.wire.frame.SubscribeFrame;
import de.lancom.systems.stomp.core.wire.frame.UnsubscribeFrame;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Stomp subscription.
 */
@Slf4j
public class StompSubscription {

    private final StompContext stompContext;
    @Getter
    private final StompConnection connection;
    @Getter
    private final SubscribeFrame subscribeFrame;
    @Getter
    private final UnsubscribeFrame unsubscribeFrame;
    @Getter
    private final StompFrameContextHandler handler;
    @Getter
    private Promise<Void> subscriptionPromise;

    /**
     * Create a new stomp subscription.
     *
     * @param stompContext stomp context
     * @param connection connection
     * @param id id
     * @param destination destination
     * @param handler handler
     */
    public StompSubscription(
            final StompContext stompContext,
            final StompConnection connection,
            final String id,
            final String destination,
            final StompFrameContextHandler handler
    ) {
        this.stompContext = stompContext;
        this.connection = connection;
        this.subscribeFrame = new SubscribeFrame(id, destination);
        this.unsubscribeFrame = new UnsubscribeFrame(id);
        this.handler = handler;
    }

    /**
     * Get id.
     *
     * @return id
     */
    public String getId() {
        return this.subscribeFrame.getId();
    }

    /**
     * Get destination.
     *
     * @return destination
     */
    public String getDestination() {
        return this.subscribeFrame.getDestination();
    }

    /**
     * Check subscription state.
     *
     * @return subscribed
     */
    public boolean isSubscribed() {
        return this.subscriptionPromise != null && this.getSubscriptionPromise().isDone();
    }

    /**
     * Reset subscription.
     */
    public void reset() {
        this.subscriptionPromise = null;
    }

    /**
     * Subscribe.
     *
     * @return promise
     */
    public Promise<Void> subscribe() {
        synchronized (this) {

            if (this.subscriptionPromise == null) {
                final Deferred<Void> deferred = this.stompContext.getDeferred().defer();
                this.subscriptionPromise = deferred.getPromise();

                if (log.isDebugEnabled()) {
                    log.debug("Subscribing as {} to {} on {}", this.getId(), this.getDestination(), connection);
                }

                this.connection.transmitFrame(subscribeFrame).then(c -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Subscribed as {} to {} on {}", this.getId(), this.getDestination(), connection);
                    }
                    this.stompContext.getSelector().wakeup();
                    return null;
                }).apply(deferred);
            }
            return this.subscriptionPromise;
        }
    }

    /**
     * Unsubscribe.
     *
     * @return promise
     */
    public Promise<Void> unsubscribe() {
        if (connection.getState() == StompConnection.State.AUTHORIZED) {
            this.subscriptionPromise = null;
            return stompContext.getDeferred().success();
        } else {
            if (this.subscriptionPromise != null && this.subscriptionPromise.isDone()) {
                return this.connection.transmitFrame(unsubscribeFrame).always(() -> {
                    this.subscriptionPromise = null;
                });
            } else {
                return stompContext.getDeferred().success();
            }
        }
    }
}
