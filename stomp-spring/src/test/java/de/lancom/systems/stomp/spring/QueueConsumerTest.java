package de.lancom.systems.stomp.spring;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.client.StompUrl;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptors;
import de.lancom.systems.stomp.core.wire.StompAckMode;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import de.lancom.systems.stomp.spring.annotation.Subscription;
import de.lancom.systems.stomp.test.AsyncHolder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { TestConfiguration.class, QueueConsumerTest.TestBed.class})
public class QueueConsumerTest extends BaseTest {

    public static final String URL_QUEUE_ACK = "${broker.url}/queue/f9c786bf-9553-4538-bc6f-a87177c6c67d";
    public static final String URL_QUEUE_NACK = "${broker.url}/queue/f196be8b-4d58-434a-bd33-20ab259d26d7";
    public static final String URL_QUEUE_CUSTOM = "${broker.url}/queue/1e060cb9-5779-4d1b-8829-afc732bb0b67";

    private static final int WAIT_SECONDS = 5;

    private static boolean interceptorAdded;

    @Autowired
    private Environment environment;

    @Autowired
    private TestBed testBed;

    @Test
    public void consumeQueueAck() throws Exception {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_QUEUE_ACK));

        client.addInterceptor(
                StompFrameContextInterceptors
                        .builder()
                        .hasUrl(url)
                        .hasAction("ACK")
                        .bodyAsString(testBed.queueHolderAck::set)
                        .build()
        );

        final SendFrame sendFrame = new SendFrame(url.getDestination(), "Body");

        assertTrue(
                "Send failed",
                client.transmitFrame(url, sendFrame).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertTrue(testBed.queueHolderAck.expect(1, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(testBed.queueHolderAck.getCount(), is(1));

    }

    @Test
    public void consumeQueueNack() throws Exception {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_QUEUE_NACK));

        client.addInterceptor(
                StompFrameContextInterceptors
                        .builder()
                        .hasUrl(url)
                        .hasAction("NACK")
                        .bodyAsString(testBed.queueHolderNack::set)
                        .build()
        );

        final SendFrame sendFrame = new SendFrame(url.getDestination(), "Body");

        assertTrue(
                "Send failed",
                client.transmitFrame(url, sendFrame).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertTrue(testBed.queueHolderNack.expect(1, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(testBed.queueHolderNack.getCount(), is(1));

    }

    @Test
    public void consumeCustomData() throws Exception {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_QUEUE_CUSTOM));

        assertTrue(
                "Send failed",
                client.send(url, new CustomData("Test")).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        System.out.println(this);
        assertTrue(testBed.queueHolderCustom.expect(1, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(testBed.queueHolderCustom.getCount(), is(1));
        assertThat(testBed.queueHolderCustom.get().getCustom(), is(equalTo("Test")));

    }

    @Component
    public static class TestBed {

        public final AsyncHolder<String> queueHolderAck = AsyncHolder.create();
        public final AsyncHolder<String> queueHolderNack = AsyncHolder.create();
        public final AsyncHolder<CustomData> queueHolderCustom = AsyncHolder.create();

        @Subscription(value = URL_QUEUE_ACK, ackMode = StompAckMode.CLIENT_INDIVIDUAL)
        public boolean processQueueFrame1(final StompFrame frame) {
            return true;
        }

        @Subscription(value = URL_QUEUE_NACK, ackMode = StompAckMode.CLIENT_INDIVIDUAL)
        public boolean processQueueFrame2(final String body) {
            return false;
        }

        @Subscription(value = URL_QUEUE_CUSTOM, ackMode = StompAckMode.CLIENT_INDIVIDUAL)
        public boolean processQueueFrame3(final CustomData data) {
            this.queueHolderCustom.set(data);
            System.out.println(this);
            return true;
        }
    }
}
