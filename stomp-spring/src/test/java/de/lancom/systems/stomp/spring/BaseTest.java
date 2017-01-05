package de.lancom.systems.stomp.spring;

import de.lancom.systems.stomp.core.client.StompClient;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptor;
import de.lancom.systems.stomp.core.connection.StompFrameContextInterceptors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class BaseTest {

    protected final StompFrameContextInterceptor logger = StompFrameContextInterceptors.logger();

    @Autowired
    protected StompClient client;

    @Before
    public void start() {
        this.client.removeInterceptor(StompFrameContextInterceptor.class);
        this.client.addInterceptor(logger);
    }

}
