package de.lancom.systems.stomp.test;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Stomp configuration.
 */
@Configuration
public class EmbeddedStompBrokerConfiguration {

    /**
     * Provide embedded broker bean.
     *
     * @param environment spring environment
     * @return broker
     * @throws Exception if broker fails to start
     */
    @Autowired
    @Bean(destroyMethod = "stop")
    public EmbeddedStompBroker broker(final ConfigurableEnvironment environment) throws Exception {
        final EmbeddedStompBroker embeddedStompBroker = new EmbeddedStompBroker();
        embeddedStompBroker.start();

        final Map<String, Object> properties = new HashMap<>();
        properties.put("embedded.broker.port", embeddedStompBroker.getPort());
        properties.put("embedded.broker.url", "stomp://localhost:${embedded.broker.port}");

        environment.getPropertySources().addFirst(new MapPropertySource("embedded.broker", properties));

        return embeddedStompBroker;
    }

    /**
     * Provide property placeholder configurer if none exists.
     *
     * @return configurer
     */
    @Bean
    @Conditional(PropertyPlaceholderCondition.class)
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * Find random open server port.
     *
     * @return port
     */
    private int randomServerPort() {
        try {
            final ServerSocket serverSocket = new ServerSocket(0);
            final int port = serverSocket.getLocalPort();
            serverSocket.close();

            return port;
        } catch (final Exception ex) {
            throw new RuntimeException("Could not find a free port", ex);
        }
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
}
