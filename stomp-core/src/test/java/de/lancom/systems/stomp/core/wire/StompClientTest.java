package de.lancom.systems.stomp.core.wire;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.StompClient;
import de.lancom.systems.stomp.core.wire.frame.SubscribeFrame;
import de.lancom.systems.stomp.test.AsyncHolder;
import de.lancom.systems.stomp.test.EmbeddedStompBroker;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class StompClientTest {

    protected static final EmbeddedStompBroker BROKER = new EmbeddedStompBroker();
    protected static final StompClient CLIENT = new StompClient();
    protected static final int HOLDER_TIMEOUT_SECONDS = 2;

    @BeforeClass
    public static void startBroker() throws Exception {
        BROKER.start();
        CLIENT.addInterceptor(new LoggingFrameInterceptor());
        CLIENT.start();
    }

    @AfterClass
    public static void stopBroker() throws Exception {
        CLIENT.stop();
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

        this.CLIENT.subscribe(url, subscriptionId, (u, f) -> {
            holder.set(f.getBodyAsString());
            return true;
        }).get();

        this.CLIENT.send(url, message).get();

        final String result = holder.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(result, is(notNullValue()));
        assertThat(result, is(equalTo(message)));
    }

    @Test
    public void readTopic() throws Exception {
        final StompUrl url = createStompUrl("/topic/%s", UUID.randomUUID());
        final String subscriptionId1 = UUID.randomUUID().toString();
        final String subscriptionId2 = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        final AsyncHolder<String> holder1 = AsyncHolder.create();
        final AsyncHolder<String> holder2 = AsyncHolder.create();

        this.CLIENT.subscribe(url, subscriptionId1, (u, f) -> {
            holder1.set(f.getBodyAsString());
            return true;
        }).get();

        this.CLIENT.subscribe(url, subscriptionId2, (u, f) -> {
            holder2.set(f.getBodyAsString());
            return true;
        }).get();

        this.CLIENT.send(url, message).get();

        final String result1 = holder1.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(result1, is(equalTo(message)));

        final String result2 = holder2.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(result2, is(equalTo(message)));
    }

    @Test
    public void acknowledged() throws Exception {

        final StompClient CLIENT = new StompClient();
        StompClientTest.CLIENT.start();

        final StompUrl url = createStompUrl("/queue/%s", UUID.randomUUID());
        final String subscriptionId = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        try {
            final AsyncHolder<Boolean> holder = AsyncHolder.create();

            this.CLIENT.addInterceptor((u, f) -> {
                holder.set(Boolean.TRUE);
                return f;
            }, "ACK");

            final SubscribeFrame subscribeFrame = new SubscribeFrame();
            subscribeFrame.setDestination(url.getDestination());
            subscribeFrame.setId(subscriptionId);
            subscribeFrame.setAck(StompAckMode.CLIENT_INDIVIDUAL.value());

            this.CLIENT.subscribe(url, subscribeFrame, (u, f) -> true).get();

            this.CLIENT.send(url, message).get();

            assertThat(holder.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));
        } finally {
            this.CLIENT.unsubscribe(url, subscriptionId).get();
        }
    }

    @Test
    public void notAcknowledgedManual() throws Exception {

        final StompClient CLIENT = new StompClient();
        StompClientTest.CLIENT.start();

        final StompUrl url = createStompUrl("/queue/%s", UUID.randomUUID());
        final String subscriptionId = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        final AsyncHolder<Boolean> holder = AsyncHolder.create();

        this.CLIENT.addInterceptor((u, f) -> {
            holder.set(Boolean.TRUE);
            return f;
        }, "NACK");

        final SubscribeFrame subscribeFrame = new SubscribeFrame();
        subscribeFrame.setDestination(url.getDestination());
        subscribeFrame.setId(subscriptionId);
        subscribeFrame.setAck(StompAckMode.CLIENT_INDIVIDUAL.value());

        this.CLIENT.subscribe(url, subscribeFrame, (u, f) -> false);

        this.CLIENT.send(url, message);

        assertThat(holder.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));

    }

    @Test
    public void notAcknowledgedException() throws Exception {

        final StompClient CLIENT = new StompClient();
        StompClientTest.CLIENT.start();

        final StompUrl url = createStompUrl("/queue/%s", UUID.randomUUID());
        final String subscriptionId = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        final AsyncHolder<Boolean> holder = AsyncHolder.create();

        this.CLIENT.addInterceptor((u, f) -> {
            holder.set(Boolean.TRUE);
            return f;
        }, "NACK");

        final SubscribeFrame subscribeFrame = new SubscribeFrame();
        subscribeFrame.setDestination(url.getDestination());
        subscribeFrame.setId(subscriptionId);
        subscribeFrame.setAck(StompAckMode.CLIENT_INDIVIDUAL.value());

        this.CLIENT.subscribe(url, subscribeFrame, (u, f) -> {
            throw new Exception("failed");
        }).get();

        this.CLIENT.send(url, message).get();

        assertThat(holder.get(HOLDER_TIMEOUT_SECONDS, TimeUnit.SECONDS), is(Boolean.TRUE));

    }

    private StompUrl createStompUrl(final String path, final Object... parameters) {
        return StompUrl.parse("stomp://localhost:" + BROKER.getPort() + String.format(path, parameters));
    }

    @Slf4j
    private static class LoggingFrameInterceptor implements StompFrameInterceptor {

        @Override
        public StompFrame intercept(final StompUrl url, final StompFrame frame) throws Exception {
            log.info(String.format("StompFrame intercepted: \n\t%s\n\t%s", frame, url));
            return frame;
        }

    }
}
