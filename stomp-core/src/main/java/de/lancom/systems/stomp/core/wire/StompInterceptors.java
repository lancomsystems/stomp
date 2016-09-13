package de.lancom.systems.stomp.core.wire;

import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

/**
 * Utiltity class for interceptor creation.
 */
@Slf4j
public final class StompInterceptors {

    /**
     * Utililty constructor.
     */
    private StompInterceptors() {
        super();
    }

    /**
     * Create interceptor for frame only.
     *
     * @param consumer consumer
     * @return interceptor
     */
    public static StompInterceptor forFrame(final Consumer<StompFrame> consumer) {
        return forFrame(null, consumer);
    }

    /**
     * Create interceptor for frame only.
     *
     * @param url expected url
     * @param consumer consumer
     * @return interceptor
     */
    public static StompInterceptor forFrame(final StompUrl url, final Consumer<StompFrame> consumer) {
        return (u, f) -> {
            if (url == null || url.equals(u)) {
                consumer.accept(f);
            }
            return f;
        };
    }

    /**
     * Create interceptor for frame body only.
     *
     * @param consumer consumer
     * @return interceptor
     */
    public static StompInterceptor forBody(final Consumer<byte[]> consumer) {
        return forBody(null, consumer);
    }

    /**
     * Create interceptor for frame body only.
     *
     * @param url expected url
     * @param consumer consumer
     * @return interceptor
     */
    public static StompInterceptor forBody(final StompUrl url, final Consumer<byte[]> consumer) {
        return forFrame(url, f -> consumer.accept(f.getBody()));
    }

    /**
     * Create interceptor for frame body only.
     *
     * @param consumer consumer
     * @return interceptor
     */
    public static StompInterceptor forBodyAsString(final Consumer<String> consumer) {
        return forBodyAsString(null, consumer);
    }

    /**
     * Create interceptor for frame body only.
     *
     * @param url expected url
     * @param consumer consumer
     * @return interceptor
     */
    public static StompInterceptor forBodyAsString(final StompUrl url, final Consumer<String> consumer) {
        return forFrame(url, f -> consumer.accept(f.getBodyAsString()));
    }

    /**
     * Create interceptor for logging.
     *
     * @return interceptor
     */
    public static StompInterceptor forLogging() {
        return (u, f) -> {
            if (log.isInfoEnabled()) {
                final StringBuilder builder = new StringBuilder();
                builder.append('{');
                builder.append("\n\turl: ").append(u);
                builder.append("\n\tframe: ").append(f);
                builder.append("\n}");
                log.info("Intercepted frame: {}", builder.toString());
            }
            return f;
        };
    }

}
