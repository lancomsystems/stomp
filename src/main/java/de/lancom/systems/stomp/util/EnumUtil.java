package de.lancom.systems.stomp.util;

import java.util.Objects;

/**
 * Created by fkneier on 18.08.16.
 */
public class EnumUtil {

    public static <E extends EnumValue<V>, V> E findByValue(final Class<E> enumClass, V value) {
        try {
            final E[] values = (E[]) enumClass.getMethod("values").invoke(null);
            for (final E item  : (E[]) enumClass.getMethod("values").invoke(null)) {
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
