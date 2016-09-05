package de.lancom.systems.stomp.wire;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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

    private static final Map<String, String> HEADER_ESCAPINGS = new HashMap<>();

    static {
        HEADER_ESCAPINGS.put("\\", "\\\\");
        HEADER_ESCAPINGS.put("\n", "\\n");
        HEADER_ESCAPINGS.put("\r", "\\r");
        HEADER_ESCAPINGS.put(";", "\\c");
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
        for (final Map.Entry<String, String> escaping : HEADER_ESCAPINGS.entrySet()) {
            result = value.replaceAll(Pattern.quote(escaping.getKey()), escaping.getValue());
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
        for (final Map.Entry<String, String> escaping : HEADER_ESCAPINGS.entrySet()) {
            result = value.replaceAll(Pattern.quote(escaping.getValue()), escaping.getKey());
        }
        return result;
    }

}
