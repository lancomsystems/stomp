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
public class QueueConsumerTest {

    private static final int WAIT_SECONDS = 5;

    private static final String URL_QUEUE_ACK = "${embedded.broker.url}/queue/f9c786bf-9553-4538-bc6f-a87177c6c67d";
    private static final String URL_QUEUE_NACK = "${embedded.broker.url}/queue/f196be8b-4d58-434a-bd33-20ab259d26d7";

    private static final AsyncHolder<String> QUEUE_HOLDER_ACK = AsyncHolder.create();
    private static final AsyncHolder<String> QUEUE_HOLDER_NACK = AsyncHolder.create();

    @Autowired
    private Environment environment;

    @Autowired
    private StompClient client;

    @Subscription(value = URL_QUEUE_ACK, ackMode = StompAckMode.CLIENT_INDIVIDUAL)
    public boolean processQueueFrame2(final String body) {
        return true;
    }

    @Subscription(value = URL_QUEUE_NACK, ackMode = StompAckMode.CLIENT_INDIVIDUAL)
    public boolean processQueueFrame1(final StompFrame frame) {
        return false;
    }

    @Test
    public void consumeQueueAck() throws Exception {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_QUEUE_ACK));

        client.addInterceptor(
                StompFrameContextInterceptors
                        .builder()
                        .hasUrl(url)
                        .hasAction("ACK")
                        .bodyAsString(QUEUE_HOLDER_ACK::set)
                        .build()
        );

        final SendFrame sendFrame = new SendFrame(url.getDestination(), "Body");

        assertTrue(
                "Send failed",
                client.transmitFrame(url, sendFrame).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertTrue(QUEUE_HOLDER_ACK.expect(1, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(QUEUE_HOLDER_ACK.getCount(), is(1));

    }

    @Test
    public void consumeQueueNack() throws Exception {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_QUEUE_NACK));

        client.addInterceptor(
                StompFrameContextInterceptors
                        .builder()
                        .hasUrl(url)
                        .hasAction("NACK")
                        .bodyAsString(QUEUE_HOLDER_NACK::set)
                        .build()
        );


        final SendFrame sendFrame = new SendFrame(url.getDestination(), "Body");

        assertTrue(
                "Send failed",
                client.transmitFrame(url, sendFrame).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertTrue(QUEUE_HOLDER_NACK.expect(1, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(QUEUE_HOLDER_NACK.getCount(), is(1));

    }

}
