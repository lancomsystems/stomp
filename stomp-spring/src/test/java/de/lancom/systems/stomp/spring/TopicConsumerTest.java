package de.lancom.systems.stomp.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.lancom.systems.stomp.core.client.StompClient;
import de.lancom.systems.stomp.core.client.StompUrl;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptors;
import de.lancom.systems.stomp.core.wire.StompAckMode;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import de.lancom.systems.stomp.spring.annotation.Subscription;
import de.lancom.systems.stomp.test.AsyncHolder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ContextConfiguration(classes = TestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TopicConsumerTest {

    private static final int WAIT_SECONDS = 5;

    private static final String URL_TOPIC = "${broker.url}/topic/645f7e02-17f8-4b6e-baf4-b43b55a74784";
    private static final String URL_TOPIC_BIG = "${broker.url}/topic/645f7e02-17f8-4b6e-baf4-b43b55a74799";

    private static final AsyncHolder<String> TOPIC_HOLDER_GENERAL = AsyncHolder.create();
    private static final AsyncHolder<String> TOPIC_HOLDER_A = AsyncHolder.create();
    private static final AsyncHolder<String> TOPIC_HOLDER_B = AsyncHolder.create();
    private static final AsyncHolder<String> TOPIC_HOLDER_BIG = AsyncHolder.create();

    @Autowired
    private Environment environment;

    @Autowired
    private StompClient client;

    private static boolean interceptorAdded;

    @Subscription(value = URL_TOPIC,
                  ackMode = StompAckMode.AUTO)
    public void processTopicFrameGeneral(final StompUrl url, final StompFrame frame) {
        TOPIC_HOLDER_GENERAL.set(frame.getBodyAsString());
    }

    @Subscription(value = URL_TOPIC,
                  selector = "flag = 'a'",
                  ackMode = StompAckMode.AUTO)
    public void processTopicFrameA(final String body) {
        System.out.println("A");
        TOPIC_HOLDER_A.set(body);
    }

    @Subscription(value = URL_TOPIC,
                  selector = "flag = 'b'",
                  ackMode = StompAckMode.AUTO)
    public void processTopicFrameB(final StompFrame frame) {
        System.out.println("B");
        TOPIC_HOLDER_B.set(frame.getBodyAsString());
    }

    @Subscription(value = URL_TOPIC_BIG,
                  ackMode = StompAckMode.AUTO)
    public void processTopicFrameBig(final StompFrame frame) {
        System.out.println("big");
        TOPIC_HOLDER_BIG.set(frame.getBodyAsString());
    }

    @Before
    public void setup() {
        if (!interceptorAdded) {
            client.addInterceptor(StompFrameContextInterceptors.logger());
            interceptorAdded = true;
        }
    }

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

    @Test
    @Ignore
    public void consumeTopicFrameBig() throws Exception {
        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(URL_TOPIC_BIG));
        final String body = "[{\"_id\":\"5857feb79d286b06b7aa56b6\",\"index\":0,\"guid\":\"cc78ec9a-7783-4334-876e-1ebacdc97daa\",\"isActive\":false,\"balance\":\"$1,741.97\",\"picture\":\"http://placehold.it/32x32\",\"age\":25,\"eyeColor\":\"brown\",\"name\":\"Eve Compton\",\"gender\":\"female\",\"company\":\"XLEEN\",\"email\":\"evecompton@xleen.com\",\"phone\":\"+1 (901) 538-3882\",\"address\":\"450 King Street, Westerville, Delaware, 8688\",\"about\":\"Elit consectetur minim dolore anim adipisicing velit sunt magna laboris occaecat. Tempor nulla eiusmod aliquip sint. Ad laborum aliquip eiusmod labore laborum ullamco excepteur. Ullamco ut dolore eu deserunt aute cillum duis. Veniam voluptate fugiat dolor nisi pariatur non exercitation laborum sint elit ut cillum. Lorem amet voluptate ad excepteur sunt eu commodo laboris do est pariatur amet nisi. Tempor laboris ipsum incididunt fugiat eu.\\r\\n\",\"registered\":\"2016-07-04T05:51:58 -02:00\",\"latitude\":-50.47536,\"longitude\":-59.242146,\"tags\":[\"dolor\",\"ut\",\"duis\",\"in\",\"deserunt\",\"veniam\",\"officia\"],\"friends\":[{\"id\":0,\"name\":\"Benita Fitzpatrick\"},{\"id\":1,\"name\":\"Ilene Frank\"},{\"id\":2,\"name\":\"Byrd Dotson\"}],\"greeting\":\"Hello, Eve Compton! You have 9 unread messages.\",\"favoriteFruit\":\"banana\"},{\"_id\":\"5857feb73583211921f0924c\",\"index\":1,\"guid\":\"d338fbc7-4392-45b6-8cf2-12155fd47533\",\"isActive\":false,\"balance\":\"$2,893.74\",\"picture\":\"http://placehold.it/32x32\",\"age\":39,\"eyeColor\":\"green\",\"name\":\"Lakeisha Odonnell\",\"gender\":\"female\",\"company\":\"ZENTIME\",\"email\":\"lakeishaodonnell@zentime.com\",\"phone\":\"+1 (866) 590-3618\",\"address\":\"451 Broadway , Gibsonia, Virgin Islands, 8742\",\"about\":\"Nisi sit est in ullamco sunt est consequat enim irure velit. Incididunt aliquip minim duis adipisicing commodo qui velit officia quis reprehenderit eiusmod reprehenderit aliqua. Aliqua labore consequat voluptate qui aliqua mollit tempor ipsum aliquip enim. Anim incididunt esse adipisicing ea elit fugiat fugiat culpa sit sunt ex aute nostrud. Ipsum adipisicing laboris sit ea.\\r\\n\",\"registered\":\"2015-03-13T08:06:05 -01:00\",\"latitude\":-33.463154,\"longitude\":-75.968074,\"tags\":[\"fugiat\",\"anim\",\"minim\",\"ad\",\"commodo\",\"commodo\",\"consequat\"],\"friends\":[{\"id\":0,\"name\":\"Jimenez Wells\"},{\"id\":1,\"name\":\"Carole Roberts\"},{\"id\":2,\"name\":\"Anita Kelly\"}],\"greeting\":\"Hello, Lakeisha Odonnell! You have 3 unread messages.\",\"favoriteFruit\":\"strawberry\"},{\"_id\":\"5857feb73c85713c5f3ce4d2\",\"index\":2,\"guid\":\"bb95be2d-ea6c-4eb7-acb2-34708505931e\",\"isActive\":true,\"balance\":\"$3,935.75\",\"picture\":\"http://placehold.it/32x32\",\"age\":29,\"eyeColor\":\"brown\",\"name\":\"Ford Mcbride\",\"gender\":\"male\",\"company\":\"CHILLIUM\",\"email\":\"fordmcbride@chillium.com\",\"phone\":\"+1 (924) 575-2410\",\"address\":\"585 Emmons Avenue, Strong, Massachusetts, 1712\",\"about\":\"Sit laboris laboris consectetur dolor magna duis. Labore duis consequat sunt est adipisicing magna nulla velit esse est laboris. Sit ad do voluptate est sint amet consectetur nisi enim.\\r\\n\",\"registered\":\"2015-03-22T06:39:10 -01:00\",\"latitude\":32.928949,\"longitude\":-156.300907,\"tags\":[\"eiusmod\",\"tempor\",\"ut\",\"sunt\",\"ullamco\",\"eiusmod\",\"ut\"],\"friends\":[{\"id\":0,\"name\":\"Katy Berger\"},{\"id\":1,\"name\":\"Pugh Spencer\"},{\"id\":2,\"name\":\"Heath Buckley\"}],\"greeting\":\"Hello, Ford Mcbride! You have 4 unread messages.\",\"favoriteFruit\":\"apple\"},{\"_id\":\"5857feb77a9488286c620a95\",\"index\":3,\"guid\":\"27316ca1-48ef-4a0e-851f-99514cae2279\",\"isActive\":true,\"balance\":\"$2,043.46\",\"picture\":\"http://placehold.it/32x32\",\"age\":30,\"eyeColor\":\"green\",\"name\":\"Faulkner Mueller\",\"gender\":\"male\",\"company\":\"EVIDENDS\",\"email\":\"faulknermueller@evidends.com\",\"phone\":\"+1 (984) 486-3418\",\"address\":\"956 Harbor Lane, Roeville, Florida, 8779\",\"about\":\"Nulla proident est do ad nisi deserunt duis elit eiusmod aute reprehenderit consectetur. Velit excepteur anim laboris incididunt velit deserunt aute nulla fugiat. Id tempor reprehenderit est ea elit sint fugiat quis ullamco labore officia culpa fugiat id. Reprehenderit laborum cupidatat in labore qui esse velit.\\r\\n\",\"registered\":\"2015-01-04T05:20:53 -01:00\",\"latitude\":-48.443617,\"longitude\":18.292414,\"tags\":[\"eu\",\"do\",\"veniam\",\"anim\",\"et\",\"tempor\",\"aliquip\"],\"friends\":[{\"id\":0,\"name\":\"Melinda Mcdaniel\"},{\"id\":1,\"name\":\"Emma Tanner\"},{\"id\":2,\"name\":\"Gould Sweet\"}],\"greeting\":\"Hello, Faulkner Mueller! You have 5 unread messages.\",\"favoriteFruit\":\"banana\"},{\"_id\":\"5857feb7bc153feb86af8ce4\",\"index\":4,\"guid\":\"c3654dce-d977-43c1-acf3-4507bec93d7b\",\"isActive\":false,\"balance\":\"$2,632.33\",\"picture\":\"http://placehold.it/32x32\",\"age\":40,\"eyeColor\":\"blue\",\"name\":\"Nancy Maldonado\",\"gender\":\"female\",\"company\":\"ZOLARITY\",\"email\":\"nancymaldonado@zolarity.com\",\"phone\":\"+1 (914) 470-2245\",\"address\":\"366 Halsey Street, Kanauga, Kentucky, 5271\",\"about\":\"Quis ad amet Lorem esse nostrud nulla quis velit occaecat ea nulla. Dolor est Lorem reprehenderit ut culpa enim do fugiat aliquip fugiat anim. Ullamco mollit laborum tempor sunt aliqua tempor eu quis ipsum proident magna est et.\\r\\n\",\"registered\":\"2015-04-25T11:40:02 -02:00\",\"latitude\":79.620149,\"longitude\":83.361566,\"tags\":[\"tempor\",\"et\",\"nulla\",\"velit\",\"dolor\",\"consectetur\",\"nulla\"],\"friends\":[{\"id\":0,\"name\":\"Bradley Chapman\"},{\"id\":1,\"name\":\"Bette Shelton\"},{\"id\":2,\"name\":\"Ana Church\"}],\"greeting\":\"Hello, Nancy Maldonado! You have 8 unread messages.\",\"favoriteFruit\":\"apple\"}]";

        final SendFrame sendFrame = new SendFrame(url.getDestination(), body);
        assertTrue(
                "Send failed",
                client.transmitFrame(url, sendFrame).await(WAIT_SECONDS, TimeUnit.SECONDS)
        );

        assertTrue(TOPIC_HOLDER_BIG.expect(1, WAIT_SECONDS, TimeUnit.SECONDS));
        assertThat(TOPIC_HOLDER_BIG.getCount(), is(1));
        Assert.assertEquals(body, TOPIC_HOLDER_BIG.get());
    }

}
