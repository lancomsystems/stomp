package de.lancom.systems.stomp.core.connection;

import de.lancom.systems.stomp.core.promise.Deferred;
import lombok.Data;
import lombok.NonNull;

/**
 * Stomp frame receive job.
 */
@Data
public class StompFrameAwaitJob {
    @NonNull
    private final StompFrameContextHandler handler;
    @NonNull
    private final Deferred<StompFrameContext> deferred;
}
