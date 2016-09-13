package de.lancom.systems.stomp.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.lancom.systems.stomp.core.wire.StompAckMode;

/**
 * Annotation for stomp subscriptions.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StompSubscription {

    /**
     * Subscription id.
     *
     * @return id
     */
    String id() default "";

    /**
     * Subscription url.
     *
     * @return url
     */
    String value();

    /**
     * Subscription selector.
     *
     * @return selector
     */
    String selector() default "";

    /**
     * Subscription acknowledge mode.
     *
     * @return mode
     */
    StompAckMode ackMode() default StompAckMode.CLIENT_INDIVIDUAL;

}
