package de.lancom.systems.stomp.core.connection;

import java.util.function.BooleanSupplier;

import de.lancom.systems.stomp.core.promise.Deferred;
import lombok.Data;
import lombok.NonNull;

/**
 * Stomp frame send job.
 */
@Data
public class StompFrameTransmitJob {

    @NonNull
    private final StompFrameContext context;
    @NonNull
    private final BooleanSupplier condition;

    private final Deferred<StompFrameContext> deferred;

}
