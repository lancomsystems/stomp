package de.lancom.systems.stomp.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.client.StompClient;
import de.lancom.systems.stomp.core.client.StompUrl;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptors;
import de.lancom.systems.stomp.core.wire.StompAckMode;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import de.lancom.systems.stomp.spring.annotation.Subscription;
import de.lancom.systems.stomp.test.AsyncHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@ContextConfiguration(classes = TestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TopicConsumerTest {

    private static final int WAIT_SECONDS = 5;

    private static final String URL_TOPIC = "${broker.url}/topic/645f7e02-17f8-4b6e-baf4-b43b55a74784";

    private static final AsyncHolder<String> TOPIC_HOLDER_GENERAL = AsyncHolder.create();
    private static final AsyncHolder<String> TOPIC_HOLDER_A = AsyncHolder.create();
    private static final AsyncHolder<String> TOPIC_HOLDER_B = AsyncHolder.create();

    @Autowired
    private Environment environment;

    @Autowired
    private StompClient client;

    @Subscription(value = URL_TOPIC, ackMode = StompAckMode.AUTO)
    public void processTopicFrameGeneral(final StompUrl url, final StompFrame frame) {
        TOPIC_HOLDER_GENERAL.set(frame.getBodyAsString());
    }

    @Subscription(value = URL_TOPIC, selector = "flag = 'a'", ackMode = StompAckMode.AUTO)
    public void processTopicFrameA(final String body) {
        System.out.println("A");
        TOPIC_HOLDER_A.set(body);
    }

    @Subscription(value = URL_TOPIC, selector = "flag = 'b'", ackMode = StompAckMode.AUTO)
    public void processTopicFrameB(final StompFrame frame) {
        System.out.println("B");
        TOPIC_HOLDER_B.set(frame.getBodyAsString());
    }

    @Test
    public void consumeTopicFiltered() throws Exception {
        this.client.addInterceptor(StompFrameContextInterceptors.logger());

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

        assertTrue(TOPIC_HOLDER_A.expect(1, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(TOPIC_HOLDER_A.getCount(), is(1));
        assertThat(TOPIC_HOLDER_A.contains("Flag A"), is(true));

        assertTrue(TOPIC_HOLDER_B.expect(1, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(TOPIC_HOLDER_B.getCount(), is(1));
        assertThat(TOPIC_HOLDER_B.contains("Flag B"), is(true));

        assertTrue(TOPIC_HOLDER_GENERAL.expect(2, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(TOPIC_HOLDER_GENERAL.getCount(), is(2));
        assertThat(TOPIC_HOLDER_GENERAL.contains("Flag A"), is(true));
        assertThat(TOPIC_HOLDER_GENERAL.contains("Flag B"), is(true));

    }

}
