package de.lancom.systems.stomp.core.connection;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import de.lancom.systems.stomp.core.client.StompUrl;
import de.lancom.systems.stomp.core.wire.StompFrame;
import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

/**
 * Utiltity class for interceptor creation.
 */
@Slf4j
@Builder(builderClassName = "Builder")
public class StompFrameContextInterceptors implements StompFrameContextInterceptor {

    @Singular
    private final Collection<String> hasActions;
    @Singular
    private final Map<String, Function<String, Boolean>> hasHeaders;
    private final Map<String, Function<String, Boolean>> hasParameters;
    private final Consumer<String> bodyAsString;
    private final Consumer<byte[]> body;
    private final Consumer<String> action;
    private final Consumer<StompFrame> frame;
    private final Consumer<Boolean> match;
    private final Runnable exec;
    private final boolean logging;
    private final StompUrl hasUrl;

    /**
     * Create a new logger interceptor for the given actions.
     *
     * @param actions actions
     * @return interceptor
     */
    public static StompFrameContextInterceptors logger(final String... actions) {
        final Builder builder = StompFrameContextInterceptors.builder().logging(true);
        for (final String action : actions) {
            builder.hasAction(action);
        }
        return builder.build();
    }

    /**
     * Create a new body interceptor for the given actions.
     *
     * @param consumer consumer
     * @param actions  actions
     * @return interceptor
     */
    public static StompFrameContextInterceptors bodyAsString(
            final Consumer<String> consumer, final String... actions
    ) {
        return bodyAsString(null, consumer, actions);
    }

    /**
     * Create a new body interceptor for the given url and actions.
     *
     * @param url      url
     * @param consumer consumer
     * @param actions  actions
     * @return interceptor
     */
    public static StompFrameContextInterceptors bodyAsString(
            final StompUrl url, final Consumer<String> consumer, final String... actions
    ) {
        final Builder builder = StompFrameContextInterceptors.builder().hasUrl(url).bodyAsString(consumer);
        for (final String action : actions) {
            builder.hasAction(action);
        }
        return builder.build();
    }

    /**
     * Create a new body interceptor for the given url and actions.
     *
     * @param consumer consumer
     * @param actions  actions
     * @return interceptor
     */
    public static StompFrameContextInterceptors body(
            final Consumer<byte[]> consumer, final String... actions
    ) {
        return body(null, consumer, actions);
    }

    /**
     * Create a new body interceptor for the given url and actions.
     *
     * @param url      url
     * @param consumer consumer
     * @param actions  actions
     * @return interceptor
     */
    public static StompFrameContextInterceptors body(
            final StompUrl url, final Consumer<byte[]> consumer, final String... actions
    ) {
        final Builder builder = StompFrameContextInterceptors.builder().hasUrl(url).body(consumer);
        for (final String action : actions) {
            builder.hasAction(action);
        }
        return builder.build();
    }

    @Override
    public void intercept(final StompFrameContext context) {
        boolean handle = true;
        final StompFrame contextFrame = context.getFrame();

        if (!hasActions.isEmpty()) {
            handle = handle && hasActions.contains(contextFrame.getAction());
        }

        if (hasParameters != null) {
            for (final Map.Entry<String, Function<String, Boolean>> entry : hasParameters.entrySet()) {
                handle = handle && entry.getValue().apply(context.getParameter(entry.getKey()));
            }
        }

        if (hasHeaders != null) {
            for (final Map.Entry<String, Function<String, Boolean>> entry : hasHeaders.entrySet()) {
                handle = handle && entry.getValue().apply(context.getFrame().getHeader(entry.getKey()));
            }
        }

        if (handle) {
            if (action != null) {
                action.accept(contextFrame.getAction());
            }
            if (this.frame != null) {
                this.frame.accept(contextFrame);
            }
            if (body != null) {
                body.accept(contextFrame.getBody());
            }
            if (bodyAsString != null) {
                bodyAsString.accept(contextFrame.getBodyAsString());
            }
            if (match != null) {
                match.accept(true);
            }
            if (exec != null) {
                exec.run();
            }
            if (logging && log.isInfoEnabled()) {
                final StompUrl url = context.getParameter(StompUrl.class);

                final StringBuilder builder = new StringBuilder();
                builder.append('{');
                if (url != null) {
                    builder.append("\n\turl: ").append(url);
                }
                if (contextFrame != null) {
                    builder.append("\n\tframe: ").append(contextFrame);
                }
                builder.append("\n}");
                log.info("Intercepted frame: {}", builder.toString());
            }
        }
    }

}
