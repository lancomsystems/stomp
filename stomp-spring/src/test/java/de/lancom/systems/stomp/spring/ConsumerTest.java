package de.lancom.systems.stomp.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.StompClient;
import de.lancom.systems.stomp.core.spring.EnableStomp;
import de.lancom.systems.stomp.core.spring.StompSubscription;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompUrl;
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

    private static final String URL = "${embedded.broker.url}/topic/test";

    private static final AsyncHolder<StompFrame> HOLDER = AsyncHolder.create();

    @Autowired
    private Environment environment;

    @Autowired
    private StompClient client;

    @StompSubscription(URL)
    public boolean processFrame(final StompFrame frame) {
        HOLDER.set(frame);
        return true;
    }

    @Test
    public void test() throws Exception {
        client.send(StompUrl.parse(environment.resolvePlaceholders(URL)), "Test").get();

        final StompFrame frame = HOLDER.get(20, TimeUnit.SECONDS);

        assertThat(frame, is(notNullValue()));
        assertThat(frame.getBodyAsString(), is("Test"));

    }

}
