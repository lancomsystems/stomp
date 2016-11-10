package de.lancom.systems.stomp.spring;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import de.lancom.systems.stomp.core.client.StompClient;
import de.lancom.systems.stomp.core.client.StompUrl;
import de.lancom.systems.stomp.core.connection.StompFrameContextHandler;
import de.lancom.systems.stomp.core.connection.StompSubscription;
import de.lancom.systems.stomp.core.util.StringUtil;
import de.lancom.systems.stomp.core.wire.StompData;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompHeader;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import de.lancom.systems.stomp.core.wire.frame.SubscribeFrame;
import de.lancom.systems.stomp.spring.annotation.Destination;
import de.lancom.systems.stomp.spring.annotation.Subscription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles injection of {@link StompProducer} and creates subscriptions.
 */
@Slf4j
@SuppressWarnings("unchecked")
public class StompSupportProcessor implements
        MergedBeanDefinitionPostProcessor,
        InstantiationAwareBeanPostProcessor,
        DestructionAwareBeanPostProcessor,
        ApplicationContextAware,
        ApplicationListener<ContextRefreshedEvent> {

    private final Map<String, StompBeanInformation> informationCache = new HashMap<>();

    @Autowired
    private StompClient client;

    /**
     * Stores consumer bean classes (values) for the given bean names (keys).
     */
    private List<Pair<Object, StompBeanConsumer>> beanConsumers = new ArrayList<>();

    private ConfigurableBeanFactory beanFactory;

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        final StompBeanInformation information = this.findBeanInformation(bean.getClass(), beanName);
        for (final StompBeanConsumer consumer : information.getConsumers()) {
            this.beanConsumers.add(new Pair<>(bean, consumer));
        }

        return bean;
    }

    @Override
    public void postProcessMergedBeanDefinition(
            final RootBeanDefinition beanDefinition,
            final Class<?> beanType,
            final String beanName
    ) {
        final StompBeanInformation information = this.findBeanInformation(beanType, beanName);
        if (information != null) {
            for (final StompBeanProducer producer : information.getProducers()) {
                if (!beanDefinition.isExternallyManagedConfigMember(producer.getField())) {
                    beanDefinition.registerExternallyManagedConfigMember(producer.getField());
                }
            }
            for (final StompBeanConsumer consumer : information.getConsumers()) {
                if (!beanDefinition.isExternallyManagedConfigMember(consumer.getMethod())) {
                    beanDefinition.registerExternallyManagedConfigMember(consumer.getMethod());
                }
            }
        }
    }

    /**
     * Lookup bean information.
     *
     * @param beanClass bean class
     * @param beanName  bean name
     * @return information
     */
    private StompBeanInformation findBeanInformation(
            final Class beanClass,
            final String beanName
    ) {
        final String cacheKey = (StringUtils.hasLength(beanName) ? beanName : beanClass.getName());

        final StompBeanInformation information;
        if (informationCache.containsKey(cacheKey)) {
            information = informationCache.get(cacheKey);
        } else {
            information = new StompBeanInformation();
            informationCache.put(cacheKey, information);

            ReflectionUtils.doWithFields(beanClass, field -> {
                final Class contentType = field.getType();

                boolean use = true;
                use = use && field.getAnnotation(Destination.class) != null;
                use = use && contentType == StompProducer.class;

                if (use) {
                    information.getProducers().add(new StompBeanProducer(field));
                }
            });

            ReflectionUtils.doWithMethods(beanClass, method -> {

                final Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);

                boolean use = true;
                use = use && BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod);
                use = use && bridgedMethod.isAnnotationPresent(Subscription.class);
                use = use && method.equals(ClassUtils.getMostSpecificMethod(method, beanClass));

                if (use) {
                    information.getConsumers().add(new StompBeanConsumer(bridgedMethod));
                }
            });
        }

        return information;
    }

    @Override
    public void postProcessBeforeDestruction(final Object bean, final String beanName) throws BeansException {

    }

    @Override
    public boolean requiresDestruction(final Object bean) {
        return true;
    }

    @Override
    public Object postProcessBeforeInstantiation(
            final Class<?> beanClass,
            final String beanName
    ) throws BeansException {
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {
        final StompBeanInformation information = this.findBeanInformation(bean.getClass(), beanName);
        if (information != null) {
            for (final StompBeanProducer producer : information.getProducers()) {
                producer.apply(client, bean);
            }
            for (final StompBeanConsumer consumer : information.getConsumers()) {
                consumer.apply(client, bean);
            }
        }
        return true;
    }

    @Override
    public PropertyValues postProcessPropertyValues(
            final PropertyValues pvs, final PropertyDescriptor[] pds, final Object bean, final String beanName
    ) throws BeansException {
        return pvs;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        final ConfigurableApplicationContext context = (ConfigurableApplicationContext) applicationContext;
        this.beanFactory = context.getBeanFactory();
    }

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        final Iterator<Pair<Object, StompBeanConsumer>> it = beanConsumers.iterator();
        Pair<Object, StompBeanConsumer> pair = null;
        while (it.hasNext()) {
            try {
                pair = it.next();
                pair.getValue().apply(this.client, pair.getKey());
                it.remove();
            } catch (final Exception e) {
                if (pair != null) {
                    log.trace("Unable to register consumer {} for {}", pair.getValue(), pair.getKey(), e);
                }
            }
        }
    }

    /**
     * Bean infomration holder.
     */
    @Data
    private static class StompBeanInformation {
        private final List<StompBeanProducer> producers = new ArrayList<>();
        private final List<StompBeanConsumer> consumers = new ArrayList<>();
    }

    /**
     * Bean producer information.
     */
    @Data
    private class StompBeanProducer {
        private final Field field;

        /**
         * Apply producer to bean.
         *
         * @param stompClient stomp client
         * @param bean        bean
         */
        public void apply(final StompClient stompClient, final Object bean) {
            final Destination annotation = field.getAnnotation(Destination.class);
            final StompUrl url = StompUrl.parse(beanFactory.resolveEmbeddedValue(annotation.value()));
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, bean, Proxy.newProxyInstance(
                    bean.getClass().getClassLoader(),
                    new Class[] {
                            StompProducer.class
                    },
                    new ProducerHandler(stompClient, url)
            ));
        }
    }

    /**
     * Generic pair holder.
     *
     * @param <Key>   key type
     * @param <Value> value type
     */
    @Data
    private static class Pair<Key, Value> {
        private final Key key;
        private final Value value;
    }

    /**
     * Bean consumer information.
     */
    @Data
    private class StompBeanConsumer {
        private final Method method;

        /**
         * Apply consumer to bean.
         *
         * @param stompClient stomp client
         * @param bean        bean
         */
        public void apply(final StompClient stompClient, final Object bean) {
            final Class[] parameterTypes = method.getParameterTypes();
            final Object[] parameters = new Object[parameterTypes.length];
            final StompFrameContextHandler handler = (c) -> {
                final StompFrame frame = c.getFrame();
                for (int index = 0; index < parameterTypes.length; index++) {
                    final Class type = parameterTypes[index];
                    if (type.isAssignableFrom(frame.getClass())) {
                        parameters[index] = frame;
                    } else if (StompData.class.isAssignableFrom(type)) {
                        parameters[index] = frame.copy(type);
                    } else if (type == String.class) {
                        parameters[index] = frame.getBodyAsString();
                    } else if (type == byte[].class) {
                        parameters[index] = frame.getBody();
                    } else {
                        parameters[index] = c.getParameter(type);
                    }
                }

                try {
                    final Object result = method.invoke(bean, parameters);
                    if (result instanceof Boolean) {
                        return (Boolean) result;
                    } else {
                        return true;
                    }
                } catch (final Exception ex) {
                    return false;
                }
            };

            final Subscription annotation = method.getAnnotation(Subscription.class);

            final StompUrl url = StompUrl.parse(beanFactory.resolveEmbeddedValue(annotation.value()));
            final String id = beanFactory.resolveEmbeddedValue(annotation.id());

            final StompSubscription subscription = stompClient.createSubscription(url, id, handler);

            final SubscribeFrame subscribeFrame = subscription.getSubscribeFrame();
            subscribeFrame.setId(StringUtil.isBlank(id) ? UUID.randomUUID().toString() : id);
            subscribeFrame.setDestination(url.getDestination());
            subscribeFrame.setAckMode(annotation.ackMode());

            if (!StringUtil.isBlank(annotation.selector())) {
                subscribeFrame.setSelector(annotation.selector());
            }

            subscription.subscribe();
        }
    }

    /**
     * Handler for {@link StompProducer} instances.
     */
    @AllArgsConstructor
    private static class ProducerHandler implements InvocationHandler {

        @NonNull
        private final StompClient client;

        @NonNull
        private final StompUrl url;

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            switch (method.getName()) {
            case "send": {
                final Object value = args[0];

                if (value != null) {
                    final SendFrame sendFrame;
                    if (value instanceof SendFrame) {
                        sendFrame = (SendFrame) value;
                    } else {
                        sendFrame = new SendFrame();
                        if (value instanceof StompData) {
                            StompData.class.cast(value).copy(sendFrame);
                        } else if (value instanceof String) {
                            sendFrame.setBodyAsString((String) value);
                        } else if (value instanceof byte[]) {
                            sendFrame.setBody((byte[]) value);
                        } else {
                            throw new RuntimeException(String.format(
                                    "Send body of type %s is not supported",
                                    value.getClass()
                            ));
                        }
                    }

                    if (sendFrame.getHeader(StompHeader.DESTINATION) == null) {
                        sendFrame.setDestination(url.getDestination());
                    }
                    return client.transmitFrame(url, sendFrame);
                }
            }
            case "toString": {
                return String.format("Stomp Producer for '%s'", url);
            }
            default: {
                throw new UnsupportedOperationException(String.format("Method %s is not implemented ", method));
            }
            }
        }
    }

}
