package de.lancom.systems.stomp.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompUrl;
import lombok.Data;
import lombok.NonNull;

/**
 * Message consisting of url and frame.
 */
@Data
public class StompMessage {

    private final Map<String, Object> meta = new HashMap<>();
    private final StompUrl url;
    private final StompFrame frame;
    private final StompMessage cause;

    /**
     * Create new message.
     *
     * @param url url
     * @param frame frame
     */
    public StompMessage(final StompUrl url, final StompFrame frame) {
        this(url, frame, null);
    }

    /**
     * Create new message caused by another message.
     *
     * @param url
     * @param frame
     * @param cause
     */
    public StompMessage(@NonNull final StompUrl url, @NonNull final StompFrame frame, final StompMessage cause) {
        this.url = url;
        this.frame = frame;
        this.cause = cause;
    }

    /**
     * Get message meta data.
     *
     * @param name name
     * @param <T> data type
     * @return data
     */
    public <T> T getMeta(final String name) {
        return (T) meta.get(name);
    }

    /**
     * Set message meta data.
     *
     * @param name name
     * @param data data
     */
    public void setMeta(final String name, final Object data) {
        meta.put(name, data);
    }

    /**
     * List message meta data.
     *
     * @return meta data entries
     */
    public Set<Map.Entry<String, Object>> metaEntrySet() {
        return meta.entrySet();
    }

}
