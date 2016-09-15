package de.lancom.systems.stomp.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import de.lancom.systems.stomp.core.client.StompClient;
import de.lancom.systems.stomp.core.client.StompUrl;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptor;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptors;
import de.lancom.systems.stomp.core.wire.StompHeader;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import de.lancom.systems.stomp.spring.annotation.Destination;
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
public class ProducerTest {

    private static final int WAIT_SECONDS = 5;

    private static final String URL = "${embedded.broker.url}/topic/ff550add-01ca-4181-97dc-6c64457cdf57";

    @Autowired
    private Environment environment;

    @Autowired
    private StompClient client;

    @Destination(URL)
    private StompProducer<SendFrame> producer1;

    @Destination(URL)
    private StompProducer<String> producer2;

    @Destination(URL)
    private StompProducer<byte[]> producer3;

    @Test
    public void produceFrame() {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL));

        final AsyncHolder<String> holder = AsyncHolder.create();

        final SendFrame sendFrame = new SendFrame();
        sendFrame.setBodyAsString("Test1");

        client.addInterceptor(createSendInterceptor(holder::set, url.getDestination()));

        assertTrue(
                "Send failed",
                producer1.send(sendFrame).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertThat(holder.get(1, WAIT_SECONDS, TimeUnit.SECONDS), is("Test1"));

    }

    @Test
    public void produceFrameDifferentDestination() {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL));
        final AsyncHolder<String> holder = AsyncHolder.create();

        final SendFrame sendFrame = new SendFrame();
        sendFrame.setBodyAsString("Test2");
        sendFrame.setDestination("/topic/7f0b8579-afac-4173-a721-058c253fc0c6");

        client.addInterceptor(createSendInterceptor(holder::set, sendFrame.getDestination()));

        assertTrue(
                "Send failed",
                producer1.send(sendFrame).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertThat(holder.get(1, WAIT_SECONDS, TimeUnit.SECONDS), is("Test2"));

    }

    @Test
    public void produceString() {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL));
        final AsyncHolder<String> holder = AsyncHolder.create();

        client.addInterceptor(createSendInterceptor(holder::set, url.getDestination()));

        assertTrue(
                "Send failed",
                producer2.send("Test3").await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertThat(holder.get(1, WAIT_SECONDS, TimeUnit.SECONDS), is("Test3"));

    }

    @Test
    public void produceByteArray() {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL));
        final AsyncHolder<String> holder = AsyncHolder.create();

        client.addInterceptor(createSendInterceptor(holder::set, url.getDestination()));

        assertTrue(
                "Send failed",
                producer3.send("Test4".getBytes(StandardCharsets.UTF_8)).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertThat(holder.get(1, WAIT_SECONDS, TimeUnit.SECONDS), is("Test4"));

    }

    private StompFrameContextInterceptor createSendInterceptor(
            final Consumer<String> consumer,
            final String destination
    ) {
        return StompFrameContextInterceptors.builder()
                .hasAction("SEND")
                .hasHeader(StompHeader.DESTINATION.value(), destination)
                .bodyAsString(consumer)
                .build();
    }

}
