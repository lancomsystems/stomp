package de.lancom.systems.stomp.core.wire;

import static de.lancom.systems.stomp.core.wire.Constants.BROKER;
import static de.lancom.systems.stomp.core.wire.Constants.CONTEXT;
import static de.lancom.systems.stomp.core.wire.Constants.TIMEOUT_SECONDS;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.lancom.systems.defer.Promise;
import de.lancom.systems.stomp.core.connection.StompConnection;
import de.lancom.systems.stomp.core.connection.StompFrameContext;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptors;
import de.lancom.systems.stomp.core.connection.StompSubscription;
import de.lancom.systems.stomp.test.AsyncHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class StompConnectionTest {

    private StompConnection connection  = new StompConnection(CONTEXT, "localhost", BROKER.getPort());

    @BeforeClass
    public static void startBroker() throws Exception {
        BROKER.start();
    }

    @AfterClass
    public static void stopBroker() throws Exception {
        BROKER.stop();
    }

    @Test
    public void sendMessage() throws Exception {
        final String destination = String.format("/topic/%s", UUID.randomUUID());

        assertTrue(
                "Send failed",
                connection.send(destination, "Test").await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        );
    }

    @Test
    public void readQueue() throws Exception {
        final String destination = String.format("/queue/%s", UUID.randomUUID());
        final String message = randomUUID().toString();

        final AsyncHolder<String> holder = AsyncHolder.create();

        final StompSubscription subscription = connection.createSubscription(destination, c -> {
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
                    connection.send(destination, message).await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
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

        final StompSubscription subscription1 = connection.createSubscription(destination, c -> {
            holder1.set(c.getFrame().getBodyAsString());
            return true;
        });

        final StompSubscription subscription2 = connection.createSubscription(destination, c -> {
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
                    connection.send(destination, message).await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            final String result1 = holder1.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(result1, is(equalTo(message)));

            final String result2 = holder2.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(result2, is(equalTo(message)));
        } finally {
            connection.removeSubscription(subscription1.getId());
            connection.removeSubscription(subscription2.getId());
        }
    }

    @Test
    public void acknowledged() throws Exception {

        final String destination = String.format("/queue/%s", UUID.randomUUID());
        final String message = randomUUID().toString();

        final StompSubscription subscription = connection.createSubscription(destination, c -> true);

        try {
            final AsyncHolder<Boolean> holder = AsyncHolder.create();

            connection.addInterceptor(
                    StompFrameContextInterceptors.builder().hasAction("ACK").match(holder::set).build()
            );
            subscription.getSubscribeFrame().setAckMode(StompAckMode.CLIENT_INDIVIDUAL);
            assertTrue(
                    "Subscription failed",
                    subscription.subscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            connection.send(destination, message).get();

            assertThat(holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));
        } finally {
            connection.removeSubscription(subscription.getId());
        }
    }

    @Test
    public void notAcknowledgedManual() throws Exception {

        final String destination = String.format("/queue/%s", UUID.randomUUID());
        final String subscriptionId = randomUUID().toString();
        final String message = randomUUID().toString();

        try {
            final AsyncHolder<Boolean> holder = AsyncHolder.create();

            connection.addInterceptor(StompFrameContextInterceptors.builder()
                                              .hasAction("NACK")
                                              .match(holder::set)
                                              .build());

            final StompSubscription subscription = connection.createSubscription(
                    subscriptionId,
                    destination,
                    c -> false
            );
            subscription.getSubscribeFrame().setAckMode(StompAckMode.CLIENT_INDIVIDUAL);
            assertTrue(
                    "Subscription failed",
                    subscription.subscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            connection.send(destination, message);

            assertThat(holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));
        } finally {
            connection.removeSubscription(subscriptionId);
        }
    }

    @Test
    public void notAcknowledgedException() throws Exception {

        final String destination = String.format("/queue/%s", UUID.randomUUID());
        final String subscriptionId = randomUUID().toString();
        final String message = randomUUID().toString();

        try {
            final AsyncHolder<Boolean> holder = AsyncHolder.create();

            connection.addInterceptor(StompFrameContextInterceptors.builder()
                                              .hasAction("NACK")
                                              .match(holder::set)
                                              .build());

            final StompSubscription subscription = connection.createSubscription(subscriptionId, destination, c -> {
                throw new Exception("Failed");
            });
            subscription.getSubscribeFrame().setAckMode(StompAckMode.CLIENT_INDIVIDUAL);

            assertTrue(
                    "Subscription failed",
                    subscription.subscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            connection.send(destination, message);

            assertThat(holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));
        } finally {
            connection.removeSubscription(subscriptionId);
        }

    }

    @Test
    @Ignore
    public void lazyConnect() throws Exception {

        final String destination = String.format("/queue/%s", UUID.randomUUID());
        final String message = randomUUID().toString();

        final AsyncHolder<String> holder = AsyncHolder.create();

        final StompSubscription subscription = connection.createSubscription(destination, c -> {
            holder.set(c.getFrame().getBodyAsString());
            return true;
        });

        try {
            connection.disconnect();
            BROKER.stop();

            subscription.subscribe();

            BROKER.start();

            assertTrue(
                    "Subscription failed",
                    subscription.getSubscriptionPromise().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            assertTrue(
                    "Send failed",
                    connection.send(destination, message).await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
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

        final StompSubscription subscription = connection.createSubscription(destination, c -> {
            holder.set(c.getFrame().getBodyAsString());
            return true;
        });

        try {

            assertTrue(
                    "Subscription failed",
                    subscription.subscribe().await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );

            BROKER.stop();

            final Promise<StompFrameContext> promise = connection.send(destination, message);

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

    private StompConnection createConnection() {
        final StompConnection connection = new StompConnection(CONTEXT, "localhost", BROKER.getPort());
        connection.addInterceptor(StompFrameContextInterceptors.logger());

        return connection;
    }
}
