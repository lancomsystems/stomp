package de.lancom.systems.stomp.core.wire;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Base for all stomp frames.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class StompFrame extends StompData {

    @NonNull
    private final String action;

    @Override
    public String toString() {
        return String.format(
                "%s(action=%s, body=%s, headers=%s)",
                this.getClass().getSimpleName(),
                action,
                this.getBodyAsString(),
                this.getHeaders()
        );
    }
}
