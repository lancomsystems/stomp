package de.lancom.systems.stomp.core.util;

/**
 * Interface for enums with value.
 *
 * @param <T> enum type
 */
public interface EnumValue<T> {

    /**
     * Get value.
     *
     * @return value
     */
    T value();
}
