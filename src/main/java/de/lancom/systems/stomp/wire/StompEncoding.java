package de.lancom.systems.stomp.wire;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by fkneier on 18.08.16.
 */
public class StompEncoding {

    public static final byte HEADER_SEPARATOR = 58;
    public static final byte CARRIAGE_RETURN = 13;
    public static final byte LINE_FEED = 10;
    public static final byte TERMINATOR = 0;


    private static final Map<String, String> HEADER_ESCAPINGS = new HashMap<>();

    static {
        HEADER_ESCAPINGS.put("\\", "\\\\");
        HEADER_ESCAPINGS.put("\n", "\\n");
        HEADER_ESCAPINGS.put("\r", "\\r");
        HEADER_ESCAPINGS.put(";", "\\c");
    }

    public static String encodeHeaderValue(final String value) {
        String result = value;
        for (final Map.Entry<String, String> escaping : HEADER_ESCAPINGS.entrySet()) {
            result = value.replaceAll(Pattern.quote(escaping.getKey()), escaping.getValue());
        }
        return result;
    }

    public static String decodeHeaderValue(final String value) {
        String result = value;
        for (final Map.Entry<String, String> escaping : HEADER_ESCAPINGS.entrySet()) {
            result = value.replaceAll(Pattern.quote(escaping.getValue()), escaping.getKey());
        }
        return result;
    }

}
