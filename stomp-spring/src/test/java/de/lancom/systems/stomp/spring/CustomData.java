package de.lancom.systems.stomp.spring;

import de.lancom.systems.stomp.core.wire.StompData;

public class CustomData extends StompData {
    public CustomData() {
    }

    public CustomData(final String custom) {
        this.setCustom(custom);
    }

    public String getCustom() {
        return this.getHeader("custom");
    }

    public void setCustom(final String custom) {
        this.setHeader("custom", custom);
    }
}
