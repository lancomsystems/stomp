package de.lancom.systems.stomp.core.connection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.lancom.systems.stomp.core.wire.StompFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Stomp frame context.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "frame")
public class StompFrameContext {

    private final Map<String, Object> parameters = new HashMap<>();

    private StompFrame frame;

    /**
     * Get parameter value using name.
     *
     * @param name name
     * @param <T> value type
     * @return value
     */
    public <T> T getParameter(final String name) {
        return (T) parameters.get(name);
    }

    /**
     * Get parameter value using class name.
     *
     * @param name class name
     * @param <T> value type
     * @return value
     */
    public <T> T getParameter(final Class<T> name) {
        return this.getParameter(name.getName());
    }

    /**
     * Set parameter value using name.
     *
     * @param name name
     * @param value value
     */
    public void setParameter(final String name, final Object value) {
        parameters.put(name, value);
    }

    /**
     * Set parameter value using class name.
     *
     * @param value value
     */
    public void setParameter(final Object value) {
        if (value != null) {
            parameters.put(value.getClass().getName(), value);
        }
    }

    /**
     * Remove parameter value using name.
     *
     * @param name name
     * @param <T> value
     * @return removed value
     */
    public <T> T removeParameter(final String name) {
        return (T) parameters.remove(name);
    }

    /**
     * Remove parameter value using class name.
     *
     * @param name name
     * @param <T> value
     * @return removed value
     */
    public <T> T removeParameter(final Class<T> name) {
        return this.removeParameter(name.getName());
    }

    /**
     * Get all parameter entries.
     *
     * @return parameter entries
     */
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

}
