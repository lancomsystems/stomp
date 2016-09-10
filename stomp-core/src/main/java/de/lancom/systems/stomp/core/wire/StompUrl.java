package de.lancom.systems.stomp.core.wire;

import java.net.URI;

import de.lancom.systems.stomp.core.util.StringUtil;

/**
 * Stomp url.
 */
public final class StompUrl {

    private final URI uri;

    /**
     * Parse the given stomp url.
     *
     * @param url url
     * @return url
     */
    public static StompUrl parse(final String url) {
        if (url == null) {
            return null;
        } else {
            return new StompUrl(url);
        }
    }

    /**
     * Create a new stomp url for the given url string.
     *
     * @param url url string
     */
    private StompUrl(final String url) {
        this.uri = URI.create(url);
    }

    /**
     * Get url scheme.
     *
     * @return scheme
     */
    public String getScheme() {
        return uri.getScheme();
    }

    /**
     * Get url host.
     *
     * @return url host
     */
    public String getHost() {
        return uri.getHost();
    }

    /**
     * Get url port.
     *
     * @return port
     */
    public int getPort() {
        return uri.getPort();
    }

    /**
     * Get url login.
     *
     * @return login
     */
    public String getLogin() {
        final String auth = uri.getAuthority();
        if (auth != null) {
            final String[] parts = auth.split(":", 2);
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return null;
    }

    /**
     * Get url passcode.
     *
     * @return passcode
     */
    public String getPasscode() {
        final String auth = uri.getAuthority();
        if (auth != null) {
            final String[] parts = auth.split(":", 2);
            if (parts.length > 1) {
                return parts[0];
            }
        }
        return null;
    }

    /**
     * Get url destination.
     *
     * @return destination
     */
    public String getDestination() {
        return uri.getPath();
    }

    /**
     * Get url base.
     *
     * @return base
     */
    public StompUrl getBase() {
        return StompUrl.parse(getBaseString());
    }

    /**
     * Create new stomp url with the given destination.
     *
     * @param destination destination
     * @return url
     */
    public StompUrl withDestination(final String destination) {
        return StompUrl.parse(getBaseString() + (StringUtil.isBlank(destination) ? "" : destination));
    }

    /**
     * Get base as string.
     *
     * @return base
     */
    private String getBaseString() {
        final String string = uri.toString();
        return string.substring(0, string.length() - getDestination().length());
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof StompUrl) {
            return uri.equals(StompUrl.class.cast(obj).uri);
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public String toString() {
        return uri.toString();
    }
}
