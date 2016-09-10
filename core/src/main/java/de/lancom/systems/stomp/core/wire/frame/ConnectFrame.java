package de.lancom.systems.stomp.core.wire.frame;

import de.lancom.systems.stomp.core.wire.StompAction;
import de.lancom.systems.stomp.core.wire.StompFrame;
import de.lancom.systems.stomp.core.wire.StompHeader;

/**
 * Connect frame.
 */
public class ConnectFrame extends StompFrame {

    /**
     * Default constructor.
     */
    public ConnectFrame() {
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
     * Get version.
     *
     * @return version
     */
    public String getAcceptVersion() {
        return this.getHeaders().get(StompHeader.ACCEPT_VERSION.value());
    }

    /**
     * Set accept version.
     *
     * @param version version
     */
    public void setAcceptVersion(final String version) {
        this.getHeaders().put(StompHeader.ACCEPT_VERSION.value(), version);
    }
}
