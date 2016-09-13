package de.lancom.systems.stomp.spring;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import de.lancom.systems.stomp.core.StompClient;
import de.lancom.systems.stomp.core.util.StringUtil;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompFrameHandler;
import de.lancom.systems.stomp.core.wire.StompHeader;
import de.lancom.systems.stomp.core.wire.StompUrl;
import de.lancom.systems.stomp.core.wire.frame.SendFrame;
import de.lancom.systems.stomp.core.wire.frame.SubscribeFrame;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

/**
 * Handles injection of {@link StompProducer} and creates subscriptions.
 */
public class StompBeanPostProcessor implements BeanPostProcessor, ApplicationListener<ContextRefreshedEvent> {

    private List<Object> beans = new ArrayList<>();

    @Autowired
    private StompClient client;

    @Autowired
    private Environment environment;

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
                final Class contentType = field.getType();
                final StompDestination annotation = field.getAnnotation(StompDestination.class);

                if (contentType == StompProducer.class && annotation != null) {
                    final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(annotation.value()));
                    ReflectionUtils.makeAccessible(field);
                    ReflectionUtils.setField(field, bean, Proxy.newProxyInstance(
                            bean.getClass().getClassLoader(),
                            new Class[] {
                                    StompProducer.class
                            },
                            new ProducerHandler(client, url)
                    ));
                }
            }
        });
        beans.add(bean);
        return bean;
    }

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        final Iterator<Object> iterator = beans.iterator();
        while (iterator.hasNext()) {
            final Object bean = iterator.next();
            iterator.remove();

            ReflectionUtils.doWithMethods(bean.getClass(), new ReflectionUtils.MethodCallback() {
                @Override
                public void doWith(final Method method) throws IllegalArgumentException, IllegalAccessException {
                    final StompSubscription annotation = method.getAnnotation(StompSubscription.class);
                    if (annotation != null) {

                        final Class returnType = method.getReturnType();
                        final Class[] parameterTypes = method.getParameterTypes();

                        final Class[] handleFrame = {
                                StompFrame.class
                        };
                        final Class[] handleString = {
                                String.class
                        };
                        final Class[] handleUrlAndFrame = {
                                StompUrl.class, StompFrame.class
                        };

                        final StompFrameHandler handler;
                        if (returnType == boolean.class) {
                            if (Arrays.equals(parameterTypes, handleFrame)) {
                                handler = (u, f) -> (boolean) method.invoke(bean, f);
                            } else if (Arrays.equals(parameterTypes, handleString)) {
                                handler = (u, f) -> (boolean) method.invoke(bean, f.getBodyAsString());
                            } else if (Arrays.equals(parameterTypes, handleUrlAndFrame)) {
                                handler = (u, f) -> (boolean) method.invoke(bean, u, f);
                            } else {
                                handler = null;
                            }
                        } else {
                            if (Arrays.equals(parameterTypes, handleFrame)) {
                                handler = (u, f) -> {
                                    method.invoke(bean, f);
                                    return true;
                                };
                            } else if (Arrays.equals(parameterTypes, handleString)) {
                                handler = (u, f) -> {
                                    method.invoke(bean, f.getBodyAsString());
                                    return true;
                                };
                            } else if (Arrays.equals(parameterTypes, handleUrlAndFrame)) {
                                handler = (u, f) -> {
                                    method.invoke(bean, u, f);
                                    return true;
                                };
                            } else {
                                handler = null;
                            }
                        }

                        if (handler == null) {
                            throw new RuntimeException(String.format("Method %s is not supported as consumer", method));
                        }

                        final StompUrl url = StompUrl.parse(environment.resolvePlaceholders(annotation.value()));
                        final String id;

                        if (!StringUtil.isBlank(annotation.id())) {
                            id = environment.resolvePlaceholders(annotation.id());
                        } else {
                            id = UUID.randomUUID().toString();
                        }

                        final SubscribeFrame frame = new SubscribeFrame();
                        frame.setId(id);
                        frame.setDestination(url.getDestination());
                        frame.setAck(annotation.ackMode().value());

                        if (!StringUtil.isBlank(annotation.selector())) {
                            frame.setSelector(annotation.selector());
                        }

                        try {
                            client.subscribe(url, frame, handler);
                        } catch (final IOException ex) {
                            throw new RuntimeException("Could not create subscription for " + annotation, ex);
                        }

                    }
                }
            });
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

                            return client.send(url, sendFrame);
                        }

                        if (!sendFrame.hasHeader(StompHeader.DESTINATION)) {
                            sendFrame.setDestination(url.getDestination());
                        }
                        return client.send(url, (SendFrame) value);
                    }
                }
                default: {
                    throw new UnsupportedOperationException(String.format("Method %s is not implemented ", method));
                }
            }
        }
    }

}
