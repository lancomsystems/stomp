package de.lancom.systems.stomp.util;

/**
 * Utility class for strings.
 */
public final class StringUtil {

    /**
     * Utility constructor.
     */
    private StringUtil() {
        super();
    }

    /**
     * Check if the value is null, empty or only contains whitespace characters.
     *
     * @param value value
     * @return empty flag
     */
    public static boolean isEmpty(final String value) {
        if (value != null) {
            for (int index = 0; index < value.length(); index++) {
                if (!Character.isWhitespace(value.charAt(index))) {
                    return false;
                }
            }
        }
        return true;
    }
}
