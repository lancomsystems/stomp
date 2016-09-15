package de.lancom.systems.stomp.spring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.lancom.systems.stomp.core.client.StompClient;
import de.lancom.systems.stomp.core.client.StompUrl;
import de.lancom.systems.stomp.core.connection.StompFrameContextHandler;
import de.lancom.systems.stomp.core.connection.StompSubscription;
import de.lancom.systems.stomp.core.util.StringUtil;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompHeader;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import de.lancom.systems.stomp.core.wire.frame.SubscribeFrame;
import de.lancom.systems.stomp.spring.annotation.Destination;
import de.lancom.systems.stomp.spring.annotation.Subscription;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

/**
 * Handles injection of {@link StompProducer} and creates subscriptions.
 */
@Slf4j
@SuppressWarnings("unchecked")
public class StompSupportProcessor implements BeanPostProcessor, EmbeddedValueResolverAware {

    @Autowired
    private StompClient client;

    private StringValueResolver resolver;

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {

        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            final Class contentType = field.getType();
            final Destination annotation = field.getAnnotation(Destination.class);

            if (contentType == StompProducer.class && annotation != null) {
                final StompUrl url = StompUrl.parse(resolver.resolveStringValue(annotation.value()));
                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.setField(field, bean, Proxy.newProxyInstance(
                        bean.getClass().getClassLoader(),
                        new Class[] {
                                StompProducer.class
                        },
                        new ProducerHandler(client, url)
                ));
            }
        }, ReflectionUtils.COPYABLE_FIELDS);

        ReflectionUtils.doWithMethods(bean.getClass(), method -> {
            final Subscription annotation = method.getAnnotation(Subscription.class);
            if (annotation != null) {

                final Class[] parameterTypes = method.getParameterTypes();
                final Object[] parameters = new Object[parameterTypes.length];
                final StompFrameContextHandler handler = (c) -> {
                    final StompFrame frame = c.getFrame();
                    for (int index = 0; index < parameterTypes.length; index++) {
                        final Class type = parameterTypes[index];
                        if (type.isAssignableFrom(frame.getClass())) {
                            parameters[index] = frame;
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

                final StompUrl url = StompUrl.parse(resolver.resolveStringValue(annotation.value()));
                final String id;

                if (!StringUtil.isBlank(annotation.id())) {
                    id = resolver.resolveStringValue(annotation.id());
                } else {
                    id = String.format("subscription-%s-%s", beanName, method.getName());
                }

                final StompSubscription subscription = client.createSubscription(url, id, handler);

                final SubscribeFrame subscribeFrame = subscription.getSubscribeFrame();
                subscribeFrame.setId(id);
                subscribeFrame.setDestination(url.getDestination());
                subscribeFrame.setAckMode(annotation.ackMode());

                if (!StringUtil.isBlank(annotation.selector())) {
                    subscribeFrame.setSelector(annotation.selector());
                }

                subscription.subscribe();
            }
        }, ReflectionUtils.USER_DECLARED_METHODS);

        return bean;
    }

    @Override
    public void setEmbeddedValueResolver(final StringValueResolver valueResolver) {
        this.resolver = valueResolver;
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

                            if (value instanceof String) {
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
                default: {
                    throw new UnsupportedOperationException(String.format("Method %s is not implemented ", method));
                }
            }
        }
    }

}
