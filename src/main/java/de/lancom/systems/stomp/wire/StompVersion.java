package de.lancom.systems.stomp.wire;

import de.lancom.systems.stomp.util.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StompVersion implements EnumValue<String> {

    VERSION_1_0("1.0"),
    VERSION_1_1("1.1"),
    VERSION_1_2("1.2");

    private final String value;

    @Override
    public String value() {
        return value;
    }
}
