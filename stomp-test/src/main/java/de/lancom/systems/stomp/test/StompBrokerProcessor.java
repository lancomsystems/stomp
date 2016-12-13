package de.lancom.systems.stomp.test;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Stomp broker processor.
 */
public class StompBrokerProcessor implements BeanPostProcessor, InitializingBean, ApplicationListener {

    @Autowired
    private ConfigurableEnvironment environment;

    @Getter
    private StompBroker broker = new StompBroker(randomServerPort());

    @Override
    public void afterPropertiesSet() throws Exception {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("broker.port", broker.getPort());
        properties.put("broker.url", "stomp://localhost:${broker.port}");

        environment.getPropertySources().addFirst(new MapPropertySource("broker", properties));

        broker.start();

        System.out.println("RUN");
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }

    /**
     * Find random open server port.
     *
     * @return port
     */
    private static int randomServerPort() {
        try {
            final ServerSocket serverSocket = new ServerSocket(0);
            final int port = serverSocket.getLocalPort();
            serverSocket.close();

            return port;
        } catch (final Exception ex) {
            throw new RuntimeException("Could not find a free port", ex);
        }
    }

    @Override
    public void onApplicationEvent(final ApplicationEvent event) {
        if (event instanceof ContextStoppedEvent) {
            try {
                broker.stop();
            } catch (final Exception ex) {
                throw new RuntimeException("Could not stop broker", ex);
            }
        }
    }
}
