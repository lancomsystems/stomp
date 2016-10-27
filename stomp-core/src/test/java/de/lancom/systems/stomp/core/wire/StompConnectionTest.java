package de.lancom.systems.stomp.core.wire;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.StompContext;
import de.lancom.systems.stomp.core.connection.StompConnection;
import de.lancom.systems.stomp.core.connection.StompFrameContext;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptors;
import de.lancom.systems.stomp.core.connection.StompSubscription;
import de.lancom.systems.stomp.core.promise.Promise;
import de.lancom.systems.stomp.test.AsyncHolder;
import de.lancom.systems.stomp.test.StompBroker;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

public class StompConnectionTest {

    protected static final StompBroker BROKER = StompBroker.run();
    protected static final StompContext CONTEXT = StompContext.run();
    protected static final StompConnection CONNECTION = new StompConnection(CONTEXT, "localhost", BROKER.getPort());
    protected static final int TIMEOUT_SECONDS = 2;

    @AfterClass
    public static void stopBroker() throws Exception {
        CONTEXT.stop();
        BROKER.stop();
    }

    @Test
    public void sendMessage() throws Exception {
        final String destination = String.format("/topic/%s", UUID.randomUUID());

        assertTrue(
                "Send failed",
                CONNECTION.send(destination, "Test").await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        );
    }

    @Test
    public void readQueue() throws Exception {
        final String destination = String.format("/queue/%s", UUID.randomUUID());
        final String message = randomUUID().toString();

        final AsyncHolder<String> holder = AsyncHolder.create();

        final StompSubscription subscription = CONNECTION.createSubscription(destination, c -> {
            holder.set(c.getFrame().getBodyAsString());
            return true;
        });

        try {
            assertTrue(
                    "Subscription failed",
                    subscription.subscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            assertTrue(
                    "Send failed",
                    CONNECTION.send(destination, message).await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            final String result = holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(result, is(notNullValue()));
            assertThat(result, is(equalTo(message)));
        } finally {
            subscription.unsubscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Test
    public void readTopic() throws Exception {
        final String destination = String.format("/topic/%s", UUID.randomUUID());
        final String message = randomUUID().toString();

        final AsyncHolder<String> holder1 = AsyncHolder.create();
        final AsyncHolder<String> holder2 = AsyncHolder.create();

        final StompSubscription subscription1 = CONNECTION.createSubscription(destination, c -> {
            holder1.set(c.getFrame().getBodyAsString());
            return true;
        });

        final StompSubscription subscription2 = CONNECTION.createSubscription(destination, c -> {
            holder2.set(c.getFrame().getBodyAsString());
            return true;
        });

        try {
            assertTrue(
                    "Subscription failed",
                    subscription1.subscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );
            assertTrue(
                    "Subscription failed",
                    subscription2.subscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            assertTrue(
                    "Send failed",
                    CONNECTION.send(destination, message).await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            final String result1 = holder1.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(result1, is(equalTo(message)));

            final String result2 = holder2.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(result2, is(equalTo(message)));
        } finally {
            CONNECTION.removeSubscription(subscription1.getId());
            CONNECTION.removeSubscription(subscription2.getId());
        }
    }

    @Test
    public void acknowledged() throws Exception {

        CONNECTION.addInterceptor(StompFrameContextInterceptors.logger());

        final String destination = String.format("/queue/%s", UUID.randomUUID());
        final String message = randomUUID().toString();

        final StompSubscription subscription = CONNECTION.createSubscription(destination, c -> true);

        try {
            final AsyncHolder<Boolean> holder = AsyncHolder.create();

            CONNECTION.addInterceptor(
                    StompFrameContextInterceptors.builder().hasAction("ACK").match(holder::set).build()
            );
            subscription.getSubscribeFrame().setAckMode(StompAckMode.CLIENT_INDIVIDUAL);
            assertTrue(
                    "Subscription failed",
                    subscription.subscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            CONNECTION.send(destination, message).get();

            assertThat(holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));
        } finally {
            CONNECTION.removeSubscription(subscription.getId());
        }
    }

    @Test
    public void notAcknowledgedManual() throws Exception {

        final String destination = String.format("/queue/%s", UUID.randomUUID());
        final String subscriptionId = randomUUID().toString();
        final String message = randomUUID().toString();

        try {
            final AsyncHolder<Boolean> holder = AsyncHolder.create();

            CONNECTION.addInterceptor(StompFrameContextInterceptors.builder()
                                              .hasAction("NACK")
                                              .match(holder::set)
                                              .build());

            final StompSubscription subscription = CONNECTION.createSubscription(
                    subscriptionId,
                    destination,
                    c -> false
            );
            subscription.getSubscribeFrame().setAckMode(StompAckMode.CLIENT_INDIVIDUAL);
            assertTrue(
                    "Subscription failed",
                    subscription.subscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            CONNECTION.send(destination, message);

            assertThat(holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));
        } finally {
            CONNECTION.removeSubscription(subscriptionId);
        }
    }

    @Test
    public void notAcknowledgedException() throws Exception {

        final String destination = String.format("/queue/%s", UUID.randomUUID());
        final String subscriptionId = randomUUID().toString();
        final String message = randomUUID().toString();

        try {
            final AsyncHolder<Boolean> holder = AsyncHolder.create();

            CONNECTION.addInterceptor(StompFrameContextInterceptors.builder()
                                              .hasAction("NACK")
                                              .match(holder::set)
                                              .build());

            final StompSubscription subscription = CONNECTION.createSubscription(subscriptionId, destination, c -> {
                throw new Exception("Failed");
            });
            subscription.getSubscribeFrame().setAckMode(StompAckMode.CLIENT_INDIVIDUAL);

            assertTrue(
                    "Subscription failed",
                    subscription.subscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            CONNECTION.send(destination, message);

            assertThat(holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));
        } finally {
            CONNECTION.removeSubscription(subscriptionId);
        }

    }

    @Test
    @Ignore
    public void lazyConnect() throws Exception {

        final String destination = String.format("/queue/%s", UUID.randomUUID());
        final String message = randomUUID().toString();

        final AsyncHolder<String> holder = AsyncHolder.create();

        final StompSubscription subscription = CONNECTION.createSubscription(destination, c -> {
            holder.set(c.getFrame().getBodyAsString());
            return true;
        });

        try {
            CONNECTION.disconnect();
            BROKER.stop();

            subscription.subscribe();

            BROKER.start();

            assertTrue(
                    "Subscription failed",
                    subscription.getSubscriptionPromise().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            assertTrue(
                    "Send failed",
                    CONNECTION.send(destination, message).await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            final String result = holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(result, is(notNullValue()));
            assertThat(result, is(equalTo(message)));
        } finally {
            subscription.unsubscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            BROKER.start();
        }
    }

    @Test
    @Ignore
    public void reconnect() throws Exception {

        final String destination = String.format("/queue/%s", UUID.randomUUID());
        final String message = randomUUID().toString();

        final AsyncHolder<String> holder = AsyncHolder.create();

        final StompSubscription subscription = CONNECTION.createSubscription(destination, c -> {
            holder.set(c.getFrame().getBodyAsString());
            return true;
        });

        try {

            assertTrue(
                    "Subscription failed",
                    subscription.subscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            BROKER.stop();

            final Promise<StompFrameContext> promise = CONNECTION.send(destination, message);

            BROKER.start();
            assertTrue(
                    "Send failed",
                    promise.await(5, TimeUnit.SECONDS)
            );

            final String result = holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(result, is(notNullValue()));
            assertThat(result, is(equalTo(message)));
        } finally {
            subscription.unsubscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            BROKER.start();
        }

    }

}
