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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {TestConfiguration.class, ProducerTest.TestBed.class})
public class ProducerTest extends BaseTest {

    public static final String URL = "${broker.url}/topic/ff550add-01ca-4181-97dc-6c64457cdf57";

    private static final int WAIT_SECONDS = 5;

    @Autowired
    private Environment environment;

    @Autowired
    private StompClient client;

    @Autowired
    private TestBed testBed;


    @Test
    public void produceFrame() {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL));

        final AsyncHolder<String> holder = AsyncHolder.create();

        final SendFrame sendFrame = new SendFrame();
        sendFrame.setBodyAsString("Test1");

        client.addInterceptor(createBodyInterceptor(holder::set, url.getDestination()));

        assertTrue(
                "Send failed",
                testBed.producer1.send(sendFrame).await(WAIT_SECONDS, TimeUnit.SECONDS)
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

        client.addInterceptor(createBodyInterceptor(holder::set, sendFrame.getDestination()));

        assertTrue(
                "Send failed",
                testBed.producer1.send(sendFrame).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertThat(holder.get(1, WAIT_SECONDS, TimeUnit.SECONDS), is("Test2"));

    }

    @Test
    public void produceString() {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL));
        final AsyncHolder<String> holder = AsyncHolder.create();

        client.addInterceptor(createBodyInterceptor(holder::set, url.getDestination()));

        assertTrue(
                "Send failed",
                testBed.producer2.send("Test3").await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertThat(holder.get(1, WAIT_SECONDS, TimeUnit.SECONDS), is("Test3"));

    }

    @Test
    public void produceByteArray() {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL));
        final AsyncHolder<String> holder = AsyncHolder.create();

        client.addInterceptor(createBodyInterceptor(holder::set, url.getDestination()));

        assertTrue(
                "Send failed",
                testBed.producer3.send("Test4".getBytes(StandardCharsets.UTF_8)).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertThat(holder.get(1, WAIT_SECONDS, TimeUnit.SECONDS), is("Test4"));

    }

    @Test
    public void produceStompData() {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL));
        final AsyncHolder<String> holder = AsyncHolder.create();

        client.addInterceptor(createHeaderInterceptor(holder::set, "custom", url.getDestination()));

        assertTrue(
                "Send failed",
                testBed.producer4.send(new CustomData("Test5")).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertThat(holder.get(1, WAIT_SECONDS, TimeUnit.SECONDS), is("Test5"));

    }

    private StompFrameContextInterceptor createBodyInterceptor(
            final Consumer<String> consumer,
            final String destination
    ) {
        return StompFrameContextInterceptors.builder()
                .hasAction("SEND")
                .hasHeader(StompHeader.DESTINATION.value(), destination::equals)
                .bodyAsString(consumer)
                .build();
    }

    private StompFrameContextInterceptor createHeaderInterceptor(
            final Consumer<String> consumer,
            final String name,
            final String destination
    ) {
        return StompFrameContextInterceptors.builder()
                .hasAction("SEND")
                .hasHeader(StompHeader.DESTINATION.value(), destination::equals)
                .frame(f -> consumer.accept(f.getHeader("custom")))
                .build();
    }

    @Component
    public static class TestBed {

        @Destination(URL)
        public StompProducer<SendFrame> producer1;

        @Destination(URL)
        public StompProducer<String> producer2;

        @Destination(URL)
        public StompProducer<byte[]> producer3;

        @Destination(URL)
        public StompProducer<CustomData> producer4;
    }

}
