package de.lancom.systems.stomp.core.spring;

import de.lancom.systems.stomp.core.StompClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stomp configuration.
 */
@Configuration
public class StompConfiguration {

    /**
     * Stomp client bean.
     *
     * @return stomp client
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public StompClient stompClient() {
        return new StompClient();
    }

    /**
     * Stomp bean post processor bean.
     *
     * @return processor
     */
    @Bean
    public StompBeanPostProcessor stompBeanPostProcessor() {
        return new StompBeanPostProcessor();
    }

}
