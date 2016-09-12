package de.lancom.systems.stomp.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.StompClient;
import de.lancom.systems.stomp.core.wire.StompFrame;
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

    private static final String URL = "${embedded.broker.url}/topic/test";

    private static final AsyncHolder<String> HOLDER_GENERAL = AsyncHolder.create();
    private static final AsyncHolder<String> HOLDER_A = AsyncHolder.create();
    private static final AsyncHolder<String> HOLDER_B = AsyncHolder.create();

    @Autowired
    private Environment environment;

    @Autowired
    private StompClient client;

    @StompSubscription(value = URL)
    public boolean processFrameGeneral(final StompFrame frame) {
        HOLDER_GENERAL.set(frame.getBodyAsString());
        return true;
    }

    @StompSubscription(value = URL, selector = "flag = 'a'")
    public boolean processFrameA(final StompFrame frame) {
        HOLDER_A.set(frame.getBodyAsString());
        return true;
    }

    @StompSubscription(value = URL, selector = "flag = 'b'")
    public boolean processFrameB(final StompFrame frame) {
        HOLDER_B.set(frame.getBodyAsString());
        return true;
    }

    @Test
    public void consumeFiltered() throws Exception {

        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL));

        final SendFrame sendFrame1 = new SendFrame();
        sendFrame1.setDestination(url.getDestination());
        sendFrame1.setHeader("flag", "a");
        sendFrame1.setBodyAsString("Flag A");
        client.send(url, sendFrame1).get();

        final SendFrame sendFrame2 = new SendFrame();
        sendFrame2.setDestination(url.getDestination());
        sendFrame2.setHeader("flag", "b");
        sendFrame2.setBodyAsString("Flag B");
        client.send(url, sendFrame2).get();

        HOLDER_A.expect(1, 2, TimeUnit.SECONDS);
        HOLDER_B.expect(1, 2, TimeUnit.SECONDS);
        HOLDER_GENERAL.expect(2, 2, TimeUnit.SECONDS);

        assertThat(HOLDER_A.getCount(), is(1));
        assertThat(HOLDER_A.get(1), is("Flag A"));

        assertThat(HOLDER_B.getCount(), is(1));
        assertThat(HOLDER_B.get(1), is("Flag B"));

        assertThat(HOLDER_GENERAL.getCount(), is(2));
        assertThat(HOLDER_GENERAL.get(1), is("Flag A"));
        assertThat(HOLDER_GENERAL.get(2), is("Flag B"));
    }

}
