package de.lancom.systems.stomp.spring;

import de.lancom.systems.stomp.core.StompContext;
import de.lancom.systems.stomp.core.client.StompClient;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Stomp support configuration.
 */
@Configuration
public class StompSupportConfiguration implements ApplicationListener<ContextStoppedEvent> {

    /**
     * Stomp context bean.
     *
     * @return context
     */
    @Bean
    public StompContext stompContext() {
        final StompContext stompContext = new StompContext();
        stompContext.start();
        return stompContext;
    }

    /**
     * Stomp client bean.
     *
     * @return stomp client
     */
    @Bean
    public StompClient stompClient() {
        return new StompClient(stompContext());
    }


    /**
     * Stomp support processor.
     *
     * @return processor
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public StompSupportProcessor stompSupportProcessor() {
        return new StompSupportProcessor();
    }


    /**
     * Provide property placeholder configurer if none exists.
     *
     * @return configurer
     */
    @Bean
    @Conditional(PropertyPlaceholderCondition.class)
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * Condition for the existence of {@link PlaceholderConfigurerSupport} bean.
     */
    private static class PropertyPlaceholderCondition implements Condition {

        @Override
        public boolean matches(
                final ConditionContext context, final AnnotatedTypeMetadata metadata
        ) {
            return context.getBeanFactory().getBeansOfType(PlaceholderConfigurerSupport.class).isEmpty();
        }

    }

    @Override
    public void onApplicationEvent(final ContextStoppedEvent event) {
        final ApplicationContext applicationContext = event.getApplicationContext();
        for (final StompContext context : applicationContext.getBeansOfType(StompContext.class).values()) {
            context.stop();
        }
    }

}
