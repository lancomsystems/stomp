package de.lancom.systems.stomp.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Stomp configuration.
 */
@Configuration
public class StompBrokerConfiguration {

    @Autowired
    private ConfigurableEnvironment environment;

    /**
     * Stomp broker processor bean.
     *
     * @return broker processor
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public StompBrokerProcessor stompBrokerProcessor() {
        return new StompBrokerProcessor();
    }

    /**
     * Provide embedded broker bean.
     *
     * @return broker
     * @throws Exception if broker fails to start
     */
    @Bean
    public StompBroker broker() throws Exception {
        return stompBrokerProcessor().getBroker();
    }

}
