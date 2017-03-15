package de.lancom.systems.stomp.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.client.StompUrl;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptor;
import de.lancom.systems.stomp.core.wire.StompAckMode;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import de.lancom.systems.stomp.spring.annotation.Subscription;
import de.lancom.systems.stomp.test.AsyncHolder;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { TestConfiguration.class, TopicConsumerTest.TestBed.class })
public class TopicConsumerTest extends BaseTest {

    private static final int WAIT_SECONDS = 10;

    public static final String URL_TOPIC = "${broker.url}/topic/645f7e02-17f8-4b6e-baf4-b43b55a74784";
    public static final String URL_TOPIC_BIG = "${broker.url}/topic/645f7e02-17f8-4b6e-baf4-b43b55a74799";
    public static final String URL_TOPIC_BINARY = "${broker.url}/topic/b75f6e54-a84c-4275-9ec5-d4e132ecd613";

    @Autowired
    private Environment environment;

    @Autowired
    private TestBed testBed;

    @Test
    public void consumeTopicFiltered() throws Exception {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_TOPIC));

        final SendFrame sendFrame1 = new SendFrame(url.getDestination(), "Flag A");
        sendFrame1.setHeader("flag", "a");

        assertTrue(
                "Send failed",
                client.transmitFrame(url, sendFrame1).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        final SendFrame sendFrame2 = new SendFrame(url.getDestination(), "Flag B");
        sendFrame2.setHeader("flag", "b");

        assertTrue(
                "Send failed",
                client.transmitFrame(url, sendFrame2).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertTrue(testBed.topicHolderA.expect(1, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(testBed.topicHolderA.getCount(), is(1));
        assertThat(testBed.topicHolderA.contains("Flag A"), is(true));

        assertTrue(testBed.topicHolderB.expect(1, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(testBed.topicHolderB.getCount(), is(1));
        assertThat(testBed.topicHolderB.contains("Flag B"), is(true));

        assertTrue(testBed.topicHolderGeneral.expect(2, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(testBed.topicHolderGeneral.getCount(), is(2));
        assertThat(testBed.topicHolderGeneral.contains("Flag A"), is(true));
        assertThat(testBed.topicHolderGeneral.contains("Flag B"), is(true));
    }

    @Test
    public void consumeTopicFrameBig() throws Exception {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_TOPIC_BIG));
        final byte[] body = new byte[100000];
        Arrays.fill(body, (byte) 100);

        final SendFrame sendFrame = new SendFrame(url.getDestination(), body);
        assertTrue(
                "Send failed",
                client.transmitFrame(url, sendFrame).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertTrue(testBed.topicHolderBig.expect(1, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(testBed.topicHolderBig.getCount(), is(1));
        assertArrayEquals(body, testBed.topicHolderBig.get());
    }

    @Test
    public void consumeTopicFrameMass() throws Exception {
        this.client.removeInterceptor(StompFrameContextInterceptor.class);

        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_TOPIC_BINARY));
        final byte[] body = new byte[10000];
        Arrays.fill(body, (byte) 100);

        for (int index = 0; index < 1000; index++) {
            final SendFrame sendFrame = new SendFrame(url.getDestination(), body);
            assertTrue(
                    "Send failed",
                    client.transmitFrame(url, sendFrame).await(WAIT_SECONDS, TimeUnit.SECONDS)
            );
        }

        testBed.topicHolderBinary.expect(1000, WAIT_SECONDS, TimeUnit.SECONDS);
        assertThat(testBed.topicHolderBinary.getCount(), is(1000));
        for (int index = 0; index < 1000; index++) {
            assertArrayEquals(
                    String.format("Entry %s has unecected value", index),
                    body,
                    testBed.topicHolderBinary.get(index + 1)
            );
        }
    }

    @Component
    public static class TestBed {

        public final AsyncHolder<String> topicHolderGeneral = AsyncHolder.create();
        public final AsyncHolder<String> topicHolderA = AsyncHolder.create();
        public final AsyncHolder<String> topicHolderB = AsyncHolder.create();
        public final AsyncHolder<byte[]> topicHolderBig = AsyncHolder.create();
        public final AsyncHolder<byte[]> topicHolderBinary = AsyncHolder.create();

        @Subscription(value = URL_TOPIC, ackMode = StompAckMode.CLIENT_INDIVIDUAL)
        public boolean processTopicFrameGeneral(final StompUrl url, final StompFrame frame) {
            topicHolderGeneral.set(frame.getBodyAsString());
            return true;
        }

        @Subscription(value = URL_TOPIC, selector = "flag = 'a'", ackMode = StompAckMode.CLIENT_INDIVIDUAL)
        public boolean processTopicFrameA(final String body) {
            topicHolderA.set(body);
            return true;
        }

        @Subscription(value = URL_TOPIC, selector = "flag = 'b'", ackMode = StompAckMode.CLIENT_INDIVIDUAL)
        public boolean processTopicFrameB(final StompFrame frame) {
            topicHolderB.set(frame.getBodyAsString());
            return true;
        }

        @Subscription(value = URL_TOPIC_BIG, ackMode = StompAckMode.CLIENT_INDIVIDUAL)
        public boolean processTopicFrameBig(final StompFrame frame) {
            topicHolderBig.set(frame.getBody());
            return true;
        }

        @Subscription(value = URL_TOPIC_BINARY, ackMode = StompAckMode.CLIENT_INDIVIDUAL)
        public boolean processTopicFrameBinary(final StompFrame frame) {
            topicHolderBinary.set(frame.getBody());
            return true;
        }

    }
}
