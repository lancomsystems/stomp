package de.lancom.systems.stomp.core.promise.callback;

/**
 * Executor callback.
 */
public interface ExecutorCallback {

    /**
     * Execute.
     *
     * @throws Exception if error occurs
     */
    void execute() throws Exception;

}
