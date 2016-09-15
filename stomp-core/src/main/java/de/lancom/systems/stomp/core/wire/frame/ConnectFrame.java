package de.lancom.systems.stomp.core.wire.frame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.lancom.systems.stomp.core.util.EnumUtil;
import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompHeader;
import de.lancom.systems.stomp.core.client.StompUrl;
import de.lancom.systems.stomp.core.wire.StompVersion;

/**
 * Connect frame.
 */
public class ConnectFrame extends StompFrame {

    /**
     * Create new connect frame.
     */
    public ConnectFrame() {
        super(StompAction.CONNECT.value());
    }

    /**
     * Create new connect frame using login and passcode.
     *
     * @param login login
     * @param passcode passcode
     */
    public ConnectFrame(final String login, final String passcode) {
        super(StompAction.CONNECT.value());
    }

    /**
     * Create new connect frame using url.
     *
     * @param url url
     */
    public ConnectFrame(final StompUrl url) {
        super(StompAction.CONNECT.value());
    }

    /**
     * Get login.
     *
     * @return login
     */
    public String getLogin() {
        return this.getHeaders().get(StompHeader.LOGIN.value());
    }

    /**
     * Set login.
     *
     * @param login login
     */
    public void setLogin(final String login) {
        this.getHeaders().put(StompHeader.LOGIN.value(), login);
    }

    /**
     * Get passcode.
     *
     * @return passcode
     */
    public String getPasscode() {
        return this.getHeaders().get(StompHeader.PASSCODE.value());
    }

    /**
     * Set passcode.
     *
     * @param passcode passcode
     */
    public void setPasscode(final String passcode) {
        this.getHeaders().put(StompHeader.PASSCODE.value(), passcode);
    }

    /**
     * Get accept version header.
     *
     * @return version
     */
    public String getAcceptVersion() {
        return this.getHeaders().get(StompHeader.ACCEPT_VERSION.value());
    }

    /**
     * Set accept version header.
     *
     * @param version version
     */
    public void setAcceptVersion(final String version) {
        this.getHeaders().put(StompHeader.ACCEPT_VERSION.value(), version);
    }

    /**
     * Get accept versions.
     *
     * @return versions
     */
    public List<String> getAcceptVersionList() {
        final String versions = this.getAcceptVersion();
        if (versions != null) {
            return Arrays.asList(versions.split("\\s*,\\s*"));
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Set accept versions.
     *
     * @param versions versions
     */
    public void setAcceptVersionList(final List<String> versions) {
        if (versions != null) {
            this.setAcceptVersion(String.join(",", versions));
        } else {
            this.setAcceptVersion(null);
        }
    }

    /**
     * Get accept stomp versions.
     *
     * @return version
     */
    public List<StompVersion> getAcceptStompVersionList() {
        final List<StompVersion> versions = new ArrayList<>();
        for (final String value : getAcceptVersionList()) {
            final StompVersion version = EnumUtil.findByValue(StompVersion.class, value);
            if (version != null) {
                versions.add(version);
            }
        }
        return versions;
    }

    /**
     * Set accept stomp versions.
     *
     * @param versions versions
     */
    public void setAcceptStompVersionList(final List<StompVersion> versions) {
        final List<String> values = new ArrayList<>();
        for (final StompVersion version : versions) {
            values.add(version.getValue());
        }

        this.setAcceptVersionList(values);
    }

}
