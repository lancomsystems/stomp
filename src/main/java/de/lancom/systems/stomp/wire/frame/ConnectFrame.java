package de.lancom.systems.stomp.wire.frame;

import de.lancom.systems.stomp.wire.StompAction;
import de.lancom.systems.stomp.wire.StompHeader;

public class ConnectFrame extends Frame {

    public ConnectFrame() {
        super(StompAction.CONNECT.value());
    }


    public String getLogin() {
        return this.getHeaders().get(StompHeader.LOGIN.value());
    }

    public void setLogin(final String login) {
        this.getHeaders().put(StompHeader.LOGIN.value(), login);
    }

    public String getPasscode() {
        return this.getHeaders().get(StompHeader.PASSCODE.value());
    }

    public void setPasscode(final String passcode) {
        this.getHeaders().put(StompHeader.PASSCODE.value(), passcode);
    }
}
