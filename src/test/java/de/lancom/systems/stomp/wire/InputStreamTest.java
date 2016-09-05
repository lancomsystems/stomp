package de.lancom.systems.stomp.wire;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.StompClient;
import de.lancom.systems.stomp.StompFameHandler;
import de.lancom.systems.stomp.util.AsyncHolder;
import de.lancom.systems.stomp.util.Broker;
import de.lancom.systems.stomp.wire.frame.Frame;
import de.lancom.systems.stomp.wire.frame.ReceiptFrame;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class InputStreamTest {

    protected static final Broker BROKER = new Broker();
    protected static final StompClient CLIENT = new StompClient();

    @BeforeClass
    public static void startBroker() throws Exception {
        BROKER.startBroker();
        CLIENT.start();
    }

    @AfterClass
    public static void stopBroker() throws Exception {
        CLIENT.stop();
        BROKER.stopBroker();
    }

    @Test
    public void sendMessage() throws Exception {
        final StompUrl url = createStompUrl("/topic/%s", UUID.randomUUID());
        final Future<StompFameExchange> result = CLIENT.send(url, "Test");
        final StompFameExchange exchange = result.get();

        assertThat(exchange, is(notNullValue()));
        assertThat(exchange.getResponse(), is(notNullValue()));
        assertThat(exchange.getResponse(), is(instanceOf(ReceiptFrame.class)));
    }

    @Test
    public void readQueue() throws Exception {
        final StompUrl url = createStompUrl("/topic/%s", UUID.randomUUID());
        final String subscriptionId = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        try {
            final AsyncHolder<String> holder = AsyncHolder.create();

            CLIENT.subscribe(url, subscriptionId, new StompFameHandler() {
                @Override
                public void handle(final Frame frame) throws Exception {
                    holder.set(frame.getBodyAsString());
                }
            });

            final Future<StompFameExchange> future = CLIENT.send(url, message);
            final StompFameExchange exchange = future.get();

            assertThat(exchange, is(notNullValue()));
            assertThat(exchange.getResponse(), is(notNullValue()));
            assertThat(exchange.getResponse(), is(instanceOf(ReceiptFrame.class)));

            final String result = holder.get(2, TimeUnit.SECONDS);

            assertThat(result, is(notNullValue()));
            assertThat(result, is(equalTo(message)));
        } finally {
            CLIENT.unsubscribe(url, subscriptionId);
        }
    }

    @Test
    public void readTopic() throws Exception {
        final StompUrl url = createStompUrl("/topic/%s", UUID.randomUUID());
        final String subscriptionId1 = UUID.randomUUID().toString();
        final String subscriptionId2 = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        try {
            final AsyncHolder<String> holder1 = AsyncHolder.create();
            final AsyncHolder<String> holder2 = AsyncHolder.create();

            CLIENT.subscribe(url, subscriptionId1, new StompFameHandler() {
                @Override
                public void handle(final Frame frame) throws Exception {
                    holder1.set(frame.getBodyAsString());
                }
            });

            CLIENT.subscribe(url, subscriptionId2, new StompFameHandler() {
                @Override
                public void handle(final Frame frame) throws Exception {
                    holder2.set(frame.getBodyAsString());
                }
            });

            final Future<StompFameExchange> future = CLIENT.send(url, message);
            final StompFameExchange exchange = future.get();

            assertThat(exchange, is(notNullValue()));
            assertThat(exchange.getResponse(), is(notNullValue()));
            assertThat(exchange.getResponse(), is(instanceOf(ReceiptFrame.class)));

            final String result1 = holder1.get(5, TimeUnit.SECONDS);
            assertThat(result1, is(notNullValue()));
            assertThat(result1, is(equalTo(message)));

            final String result2 = holder2.get(5, TimeUnit.SECONDS);
            assertThat(result2, is(notNullValue()));
            assertThat(result2, is(equalTo(message)));
        } finally {
            CLIENT.unsubscribe(url, subscriptionId1);
            CLIENT.unsubscribe(url, subscriptionId2);
        }
    }

    private StompUrl createStompUrl(final String path, final Object... parameters) {
        return StompUrl.parse("stomp://localhost:" + BROKER.getBrokerPort() + String.format(path, parameters));
    }
}
