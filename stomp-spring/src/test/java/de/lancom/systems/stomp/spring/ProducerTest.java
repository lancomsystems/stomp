package de.lancom.systems.stomp.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.StompClient;
import de.lancom.systems.stomp.core.spring.EnableStomp;
import de.lancom.systems.stomp.core.spring.StompDestination;
import de.lancom.systems.stomp.core.spring.StompProducer;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.test.AsyncHolder;
import de.lancom.systems.stomp.test.Broker;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@EnableStomp
@ContextConfiguration(classes = ConsumerTest.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ProducerTest {

    @Autowired
    private StompClient client;

    @StompDestination("stomp://localhost:9000/topic/test")
    private StompProducer<String> producer;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Broker broker() throws Exception {
        return new Broker(9000);
    }

    @Test
    public void test() {
        final AsyncHolder<StompFrame> holder = AsyncHolder.create();

        client.addInterceptor((u, f) -> {
            holder.set(f);
            return f;
        });

        producer.send("Test");

        final StompFrame frame = holder.get(2, TimeUnit.SECONDS);

        assertThat(frame, is(notNullValue()));
        assertThat(frame.getBodyAsString(), is("Test"));

    }

}
