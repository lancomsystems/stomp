package de.lancom.systems.stomp.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.StompClient;
import de.lancom.systems.stomp.core.wire.StompAckMode;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompInterceptors;
import de.lancom.systems.stomp.core.wire.StompUrl;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import de.lancom.systems.stomp.test.AsyncHolder;
import de.lancom.systems.stomp.test.EnableEmbeddedStompBroker;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@EnableStomp
@EnableEmbeddedStompBroker
@ContextConfiguration(classes = ConsumerTest.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ConsumerTest {

    private static final String URL_TOPIC = "${embedded.broker.url}/topic/b0d6bdcc-a30e-4667-a92f-5481ab6fa9bd";
    private static final String URL_QUEUE_ACK = "${embedded.broker.url}/queue/4e6ca3b9-8ddd-477a-8300-21409ea9b082";
    private static final String URL_QUEUE_NACK = "${embedded.broker.url}/queue/9a73b332-a102-450c-9157-b6e9281cf190";

    private static final AsyncHolder<String> TOPIC_HOLDER_GENERAL = AsyncHolder.create();
    private static final AsyncHolder<String> TOPIC_HOLDER_A = AsyncHolder.create();
    private static final AsyncHolder<String> TOPIC_HOLDER_B = AsyncHolder.create();

    private static final AsyncHolder<String> QUEUE_HOLDER_ACK = AsyncHolder.create();
    private static final AsyncHolder<String> QUEUE_HOLDER_NACK = AsyncHolder.create();

    @Autowired
    private Environment environment;

    @Autowired
    private StompClient client;

    @StompSubscription(value = URL_TOPIC, ackMode = StompAckMode.AUTO)
    public void processTopicFrameGeneral(final StompUrl url, final StompFrame frame) {
        TOPIC_HOLDER_GENERAL.set(frame.getBodyAsString());
    }

    @StompSubscription(value = URL_TOPIC, selector = "flag = 'a'", ackMode = StompAckMode.AUTO)
    public void processTopicFrameA(final String body) {
        TOPIC_HOLDER_A.set(body);
    }

    @StompSubscription(value = URL_TOPIC, selector = "flag = 'b'", ackMode = StompAckMode.AUTO)
    public void processTopicFrameB(final StompFrame frame) {
        TOPIC_HOLDER_B.set(frame.getBodyAsString());
    }

    @StompSubscription(value = URL_QUEUE_ACK, ackMode = StompAckMode.CLIENT_INDIVIDUAL)
    public boolean processQueueFrame2(final String body) {
        return true;
    }

    @StompSubscription(value = URL_QUEUE_NACK, ackMode = StompAckMode.CLIENT_INDIVIDUAL)
    public boolean processQueueFrame1(final StompFrame frame) {
        return false;
    }

    @Test
    public void consumeTopicFiltered() throws Exception {

        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_TOPIC));

        final SendFrame sendFrame1 = new SendFrame(url.getDestination(), "Flag A");
        sendFrame1.setHeader("flag", "a");
        client.send(url, sendFrame1).get();

        final SendFrame sendFrame2 = new SendFrame(url.getDestination(), "Flag B");
        sendFrame2.setHeader("flag", "b");
        client.send(url, sendFrame2).get();

        TOPIC_HOLDER_GENERAL.expect(2, 2, TimeUnit.SECONDS);
        TOPIC_HOLDER_A.expect(1, 2, TimeUnit.SECONDS);
        TOPIC_HOLDER_B.expect(1, 2, TimeUnit.SECONDS);

        assertThat(TOPIC_HOLDER_GENERAL.getCount(), is(2));
        assertThat(TOPIC_HOLDER_GENERAL.contains("Flag A"), is(true));
        assertThat(TOPIC_HOLDER_GENERAL.contains("Flag B"), is(true));

        assertThat(TOPIC_HOLDER_A.getCount(), is(1));
        assertThat(TOPIC_HOLDER_A.contains("Flag A"), is(true));

        assertThat(TOPIC_HOLDER_B.getCount(), is(1));
        assertThat(TOPIC_HOLDER_B.contains("Flag B"), is(true));

    }

    @Test
    public void consumeQueueAck() throws Exception {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_QUEUE_ACK));

        client.addInterceptor(StompInterceptors.forLogging());
        client.addInterceptor(StompInterceptors.forBodyAsString(url,QUEUE_HOLDER_ACK::set), "ACK");
        client.send(url, new SendFrame(url.getDestination(), "Body")).get();

        QUEUE_HOLDER_ACK.expect(1, 2, TimeUnit.SECONDS);

        assertThat(QUEUE_HOLDER_ACK.getCount(), is(1));
    }

    @Test
    public void consumeQueueNack() throws Exception {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_QUEUE_NACK));

        client.addInterceptor(StompInterceptors.forLogging());
        client.addInterceptor(StompInterceptors.forBodyAsString(url, QUEUE_HOLDER_NACK::set), "NACK");
        client.send(url, new SendFrame(url.getDestination(), "Body")).get();

        QUEUE_HOLDER_NACK.expect(1, 2, TimeUnit.SECONDS);

        assertThat(QUEUE_HOLDER_NACK.getCount(), is(1));
    }

}
