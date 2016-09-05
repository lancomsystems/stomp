package de.lancom.systems.stomp.util;

import java.util.Objects;

/**
 * Utility class for enums.
 */
public final class EnumUtil {

    /**
     * Utility constructor.
     */
    private EnumUtil() {
        super();
    }

    /**
     * Find value by value.
     *
     * @param enumClass enum class
     * @param value value
     * @param <E> enum type
     * @param <V> value type
     * @return enum instance or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <E extends EnumValue<V>, V> E findByValue(final Class<E> enumClass, final V value) {
        try {
            final E[] values = (E[]) enumClass.getMethod("values").invoke(null);
            for (final E item : (E[]) enumClass.getMethod("values").invoke(null)) {
                if (Objects.equals(item.value(), value)) {
                    return item;
                }
            }
            return null;
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
