package de.lancom.systems.stomp.core.wire;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for stomp value encoding.
 */
public final class StompEncoding {

    /**
     * Header seperator character.
     */
    public static final byte HEADER_SEPARATOR = 58;
    /**
     * Carriage return character.
     */
    public static final byte CARRIAGE_RETURN = 13;
    /**
     * Line feed character.
     */
    public static final byte LINE_FEED = 10;
    /**
     * Terminator character.
     */
    public static final byte TERMINATOR = 0;

    private static final Map<String, String> MAPPING = new LinkedHashMap<>();

    static {
        MAPPING.put("\\", "\\\\");
        MAPPING.put("\n", "\\n");
        MAPPING.put("\r", "\\r");
        MAPPING.put(":", "\\c");
    }

    /**
     * Utililty constructor.
     */
    private StompEncoding() {
        super();
    }

    /**
     * Encode header value.
     *
     * @param value value
     * @return encoded value
     */
    public static String encodeHeaderValue(final String value) {
        String result = value;
        for (final Map.Entry<String, String> mapping : MAPPING.entrySet()) {
            result = result.replace(mapping.getKey(), mapping.getValue());
        }
        return result;
    }

    /**
     * Decode header value.
     *
     * @param value value
     * @return decoded value
     */
    public static String decodeHeaderValue(final String value) {
        String result = value;
        for (final Map.Entry<String, String> mapping : MAPPING.entrySet()) {
            result = result.replace(mapping.getValue(), mapping.getKey());
        }
        return result;
    }

}
