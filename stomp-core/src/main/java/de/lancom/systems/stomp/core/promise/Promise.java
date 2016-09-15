package de.lancom.systems.stomp.core.promise;

import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.promise.callback.ConsumerCallback;
import de.lancom.systems.stomp.core.promise.callback.ExecutorCallback;
import de.lancom.systems.stomp.core.promise.callback.ProcessorCallback;
import de.lancom.systems.stomp.core.promise.callback.PromiseProcessorCallback;
import de.lancom.systems.stomp.core.promise.callback.PromiseSupplierCallback;
import de.lancom.systems.stomp.core.promise.callback.SupplierCallback;

/**
 * Asynchronous promise.
 *
 * @param <T> result type
 */
public interface Promise<T> {

    /**
     * Append new promise using success callback.
     *
     * @param success success callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> then(ProcessorCallback<T, X> success);

    /**
     * Append new promise using success and fail callback.
     *
     * @param success success callback
     * @param fail fail callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> then(ProcessorCallback<T, X> success, ProcessorCallback<Exception, X> fail);

    /**
     * Append new promise using success and fail callback.
     *
     * @param success success callback
     * @param fail fail callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> then(ProcessorCallback<T, X> success, PromiseProcessorCallback<Exception, X> fail);

    /**
     * Append new promise using success and fail callback.
     *
     * @param success success callback
     * @param fail fail callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> then(ProcessorCallback<T, X> success, PromiseSupplierCallback<X> fail);

    /**
     * Append new promise using success and fail callback.
     *
     * @param success success callback
     * @param fail fail callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> then(ProcessorCallback<T, X> success, SupplierCallback<X> fail);

    /**
     * Append new promise using success callback.
     *
     * @param success success callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> then(PromiseProcessorCallback<T, X> success);

    /**
     * Append new promise using success and fail callback.
     *
     * @param success success callback
     * @param fail fail callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> then(PromiseProcessorCallback<T, X> success, ProcessorCallback<Exception, X> fail);

    /**
     * Append new promise using success and fail callback.
     *
     * @param success success callback
     * @param fail fail callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> then(PromiseProcessorCallback<T, X> success, PromiseProcessorCallback<Exception, X> fail);

    /**
     * Append new promise using success and fail callback.
     *
     * @param success success callback
     * @param fail fail callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> then(PromiseProcessorCallback<T, X> success, PromiseSupplierCallback<X> fail);

    /**
     * Append new promise using success and fail callback.
     *
     * @param success success callback
     * @param fail fail callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> then(PromiseProcessorCallback<T, X> success, SupplierCallback<X> fail);

    /**
     * Append new void promise using success callback.
     *
     * @param success success callback
     * @return promise
     */
    Promise<Void> then(ConsumerCallback<T> success);

    /**
     * Append new void promise.
     *
     * @return promise
     */
    Promise<Void> then();

    /**
     * Append new fallback promise using value.
     *
     * @param value value
     * @return promise
     */
    Promise<T> fail(T value);

    /**
     * Append new fallback promise using value callback.
     *
     * @param callback callback
     * @return promise
     */
    Promise<T> fail(SupplierCallback<T> callback);

    /**
     * Append new fallback promise using value callback.
     *
     * @param callback callback
     * @return promise
     */
    Promise<T> fail(PromiseProcessorCallback<Exception, T> callback);

    /**
     * Append new fallback promise using value callback.
     *
     * @param callback callback
     * @return promise
     */
    Promise<Void> fail(ConsumerCallback<Exception> callback);

    /**
     * Append new fallback promise using value callback.
     *
     * @param callback callback
     * @return promise
     */
    Promise<Void> fail(ExecutorCallback callback);

    /**
     * Append new promise using callback for any result.
     *
     * @param callback callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> always(SupplierCallback<X> callback);

    /**
     * Append new promise using callback for any result.
     *
     * @param callback callback
     * @param <X> promise result type
     * @return promise
     */
    <X> Promise<X> always(PromiseSupplierCallback<X> callback);

    /**
     * Append new void promise using callback for any result.
     *
     * @param callback callback
     * @return promise
     */
    Promise<Void> always(ExecutorCallback callback);

    /**
     * Append new void promise for any result.
     *
     * @return promise
     */
    Promise<Void> always();

    /**
     * Await completion indefinitely.
     *
     * @return completion
     */
    boolean await();

    /**
     * Await completion in the given time frame.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return completion
     */
    boolean await(long timeout, TimeUnit unit);

    /**
     * Check if promise has been completed successfully.
     *
     * @return successful
     */
    boolean isSuccess();

    /**
     * Check if promise has failed.
     *
     * @return failed
     */
    boolean isFail();

    /**
     * Check if promise has been completed.
     *
     * @return completed
     */
    boolean isDone();

    /**
     * Await result indefinitely.
     *
     * @return result
     */
    T get();

    /**
     * Await result in the given time frame.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return result
     */
    T get(long timeout, TimeUnit unit);

    /**
     * Return current result.
     *
     * @return result.
     */
    T getNow();

    /**
     * Apply result to a given {@link Deferred}.
     *
     * @param deferred deffered
     * @return promise
     */
    Promise<Void> apply(Deferred<T> deferred);

}
