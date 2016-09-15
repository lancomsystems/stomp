package de.lancom.systems.stomp.core.wire;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.StompContext;
import de.lancom.systems.stomp.core.client.StompClient;
import de.lancom.systems.stomp.core.client.StompUrl;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptors;
import de.lancom.systems.stomp.core.connection.StompSubscription;
import de.lancom.systems.stomp.test.AsyncHolder;
import de.lancom.systems.stomp.test.StompBroker;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class StompClientTest {

    protected static final StompBroker BROKER = new StompBroker();
    protected static final StompContext CONTEXT = new StompContext();
    protected static final StompClient CLIENT = new StompClient(CONTEXT);
    protected static final int HOLDER_TIMEOUT_SECONDS = 2;

    @BeforeClass
    public static void startBroker() throws Exception {
        BROKER.start();
        CONTEXT.start();
    }

    @AfterClass
    public static void stopBroker() throws Exception {
        CONTEXT.stop();
        BROKER.stop();
    }

    @Test
    public void sendMessage() throws Exception {
        final StompUrl url = createStompUrl("/topic/%s", UUID.randomUUID());
        this.CLIENT.send(url, "Test").get();
    }

    @Test
    public void readQueue() throws Exception {
        final StompUrl url = createStompUrl("/queue/%s", UUID.randomUUID());
        final String subscriptionId = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        final AsyncHolder<String> holder = AsyncHolder.create();

        try {
            CLIENT.createSubscription(url, subscriptionId, c -> {
                holder.set(c.getFrame().getBodyAsString());
                return true;
            }).subscribe().await();

            CLIENT.send(url, message).await();

            final String result = holder.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(result, is(notNullValue()));
            assertThat(result, is(equalTo(message)));
        } finally {
            CLIENT.removeSubscription(url, subscriptionId);
        }
    }

    @Test
    public void readTopic() throws Exception {
        final StompUrl url = createStompUrl("/topic/%s", UUID.randomUUID());
        final String subscriptionId1 = UUID.randomUUID().toString();
        final String subscriptionId2 = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        final AsyncHolder<String> holder1 = AsyncHolder.create();
        final AsyncHolder<String> holder2 = AsyncHolder.create();

        try {
            CLIENT.createSubscription(url, subscriptionId1, c -> {
                holder1.set(c.getFrame().getBodyAsString());
                return true;
            }).subscribe().await();

            CLIENT.createSubscription(url, subscriptionId2, c -> {
                holder2.set(c.getFrame().getBodyAsString());
                return true;
            }).subscribe().await();

            CLIENT.send(url, message).await();

            final String result1 = holder1.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(result1, is(equalTo(message)));

            final String result2 = holder2.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(result2, is(equalTo(message)));
        } finally {
            CLIENT.removeSubscription(url, subscriptionId1);
            CLIENT.removeSubscription(url, subscriptionId2);
        }
    }

    @Test
    public void acknowledged() throws Exception {

        final StompUrl url = createStompUrl("/queue/%s", UUID.randomUUID());
        final String subscriptionId = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        try {
            final AsyncHolder<Boolean> holder = AsyncHolder.create();

            CLIENT.addInterceptor(StompFrameContextInterceptors.builder().hasAction("ACK").match(holder::set).build());

            final StompSubscription subscription = CLIENT.createSubscription(url, subscriptionId, c -> true);
            subscription.getSubscribeFrame().setAckMode(StompAckMode.CLIENT_INDIVIDUAL);
            subscription.subscribe().await();

            CLIENT.send(url, message).get();

            assertThat(holder.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));
        } finally {
            CLIENT.removeSubscription(url, subscriptionId);
        }
    }

    @Test
    public void notAcknowledgedManual() throws Exception {

        final StompUrl url = createStompUrl("/queue/%s", UUID.randomUUID());
        final String subscriptionId = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        try {
            final AsyncHolder<Boolean> holder = AsyncHolder.create();

            CLIENT.addInterceptor(StompFrameContextInterceptors.builder().hasAction("NACK").match(holder::set).build());

            final StompSubscription subscription = CLIENT.createSubscription(url, subscriptionId, c -> false);
            subscription.getSubscribeFrame().setAckMode(StompAckMode.CLIENT_INDIVIDUAL);
            subscription.subscribe().await();

            CLIENT.send(url, message);

            assertThat(holder.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));
        } finally {
            CLIENT.removeSubscription(url, subscriptionId);
        }
    }

    @Test
    public void notAcknowledgedException() throws Exception {


        final StompUrl url = createStompUrl("/queue/%s", UUID.randomUUID());
        final String subscriptionId = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        try {
            final AsyncHolder<Boolean> holder = AsyncHolder.create();

            CLIENT.addInterceptor(StompFrameContextInterceptors.builder().hasAction("NACK").match(holder::set).build());

            final StompSubscription subscription = CLIENT.createSubscription(url, subscriptionId, c -> {
                throw new Exception("Failed");
            });
            subscription.getSubscribeFrame().setAckMode(StompAckMode.CLIENT_INDIVIDUAL);
            subscription.subscribe().await();

            CLIENT.send(url, message);

            assertThat(holder.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));
        } finally {
            CLIENT.removeSubscription(url, subscriptionId);
        }

    }

    private StompUrl createStompUrl(final String path, final Object... parameters) {
        return StompUrl.parse("stomp://localhost:" + BROKER.getPort() + String.format(path, parameters));
    }

}
