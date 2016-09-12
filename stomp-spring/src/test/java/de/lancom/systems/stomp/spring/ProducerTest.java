package de.lancom.systems.stomp.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.StompClient;
import de.lancom.systems.stomp.core.wire.StompFrameInterceptor;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import de.lancom.systems.stomp.test.AsyncHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@EnableStomp
@ContextConfiguration(classes = ConsumerTest.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ProducerTest {

    @Autowired
    private StompClient client;

    @StompDestination("${embedded.broker.url}/topic/ff550add-01ca-4181-97dc-6c64457cdf57")
    private StompProducer<SendFrame> producer1;

    @StompDestination("${embedded.broker.url}/topic/92bb3dd5-ca1f-4864-9039-ebca78f35e83")
    private StompProducer<String> producer2;

    @StompDestination("${embedded.broker.url}/topic/d2b6104b-cd77-4609-aa16-def71b5e1fcf")
    private StompProducer<byte[]> producer3;

    @Test
    public void produceFrame() {
        final AsyncHolder<String> holder = AsyncHolder.create();

        client.removeIntercetor(StompFrameInterceptor.class);

        client.addInterceptor((u, f) -> {
            holder.set(f.getBodyAsString());
            return f;
        }, "SEND");

        final SendFrame sendFrame = new SendFrame();
        sendFrame.setBodyAsString("Test1");

        producer1.send(sendFrame);

        assertThat(holder.get(1, 2, TimeUnit.SECONDS), is("Test1"));

    }

    @Test
    public void produceString() {
        final AsyncHolder<String> holder = AsyncHolder.create();

        client.removeIntercetor(StompFrameInterceptor.class);

        client.addInterceptor((u, f) -> {
            holder.set(f.getBodyAsString());
            return f;
        }, "SEND");

        producer2.send("Test2");

        assertThat(holder.get(1, 2, TimeUnit.SECONDS), is("Test2"));

    }

    @Test
    public void produceByteArray() {
        final AsyncHolder<String> holder = AsyncHolder.create();

        StompFrameInterceptor interceptor = (u, f) -> {
            holder.set(f.getBodyAsString());
            return f;
        };
        client.removeIntercetor(StompFrameInterceptor.class);

        client.addInterceptor((u, f) -> {
            holder.set(f.getBodyAsString());
            return f;
        }, "SEND");

        producer3.send("Test3".getBytes(StandardCharsets.UTF_8));

        assertThat(holder.get(1, 2, TimeUnit.SECONDS), is("Test3"));

    }

}
