package de.lancom.systems.stomp.wire;

import java.net.URI;

/**
 * Created by fkneier on 19.08.16.
 */
public class StompUrl {
    private final URI uri;

    public static StompUrl of(final String url) {
        return new StompUrl(url);
    }

    private StompUrl(final String url) {
        this.uri = URI.create(url);
    }

    public String getScheme() {
        return uri.getScheme();
    }

    public String getHost() {
        return uri.getHost();
    }

    public int getPort() {
        return uri.getPort();
    }

    public String getUser() {
        final String auth = uri.getAuthority();
        if (auth != null) {
            final String[] parts = auth.split(":", 2);
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return null;
    }

    public String getPassword() {
        final String auth = uri.getAuthority();
        if (auth != null) {
            final String[] parts = auth.split(":", 2);
            if (parts.length > 1) {
                return parts[0];
            }
        }
        return null;
    }

    public String getDestination() {
        return uri.getPath();
    }

    public StompUrl getBase() {
        final String string = uri.toString();
        return StompUrl.of(string.substring(0, string.length() - getDestination().length() + 1));
    }

    @Override
    public String toString() {
        return uri.toString();
    }
}
