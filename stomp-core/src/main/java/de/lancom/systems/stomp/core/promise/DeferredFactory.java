package de.lancom.systems.stomp.core.promise;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import de.lancom.systems.stomp.core.promise.callback.ConsumerCallback;
import de.lancom.systems.stomp.core.promise.callback.ExecutorCallback;
import de.lancom.systems.stomp.core.promise.callback.ProcessorCallback;
import de.lancom.systems.stomp.core.promise.callback.SupplierCallback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Deferred factory.
 */
@Slf4j
public class DeferredFactory {

    private final ExecutorService executorService;

    /**
     * Create a new deferred factory using the given executor service.
     *
     * @param executorService executors ervice
     */
    public DeferredFactory(final ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Create a new {@link Deferred}.
     *
     * @param <T> result type
     * @return deferred
     */
    public <T> Deferred<T> defer() {
        return new DeferredImpl<>(this);
    }

    /**
     * Create a successful {@link Promise}.
     *
     * @return promise
     */
    public Promise<Void> success() {
        return new DeferredImpl<Void>(this, Optional.ofNullable(null), null).getPromise();
    }

    /**
     * Create a successful {@link Promise} using the given result.
     *
     * @param <T> result type
     * @param result result
     * @return promise
     */
    public <T> Promise<T> success(final T result) {
        return new DeferredImpl<>(this, Optional.ofNullable(result), null).getPromise();
    }

    /**
     * Create a failed {@link Promise} using the given exception.
     *
     * @param <T> result type
     * @param exception exception
     * @return promise
     */
    public <T> Promise<T> fail(final Exception exception) {
        return new DeferredImpl<T>(this, null, Optional.ofNullable(exception)).getPromise();
    }

    /**
     * Create a new {@link Promise} that is completed using the given callback.
     *
     * @param callback callback
     * @return promise
     */
    public Promise<Void> defer(final ExecutorCallback callback) {
        final Deferred<Void> deferred = this.defer();
        this.executorService.submit(() -> {
            try {
                callback.execute();
                deferred.resolve(null);
            } catch (final Exception ex) {
                deferred.reject(ex);
            }
        });
        return deferred.getPromise();
    }

    /**
     * Create a new {@link Promise} that is completed using the given callback.
     *
     * @param <T> result type
     * @param callback callback
     * @return promise
     */
    public <T> Promise<T> defer(final SupplierCallback<T> callback) {
        final Deferred<T> deferred = this.defer();
        this.executorService.submit(() -> {
            try {
                deferred.resolve(callback.supply());
            } catch (final Exception ex) {
                deferred.reject(ex);
            }
        });
        return deferred.getPromise();
    }

    /**
     * Deferred implementation.
     *
     * @param <T> result type
     */
    private static class DeferredImpl<T> implements Deferred<T> {
        private final DeferredFactory factory;
        private Optional<T> result;
        private Optional<Exception> exception;
        @Getter
        private List<ConsumerCallback<T>> successHandlers = new CopyOnWriteArrayList<>();
        @Getter
        private List<ConsumerCallback<Exception>> failHandlers = new CopyOnWriteArrayList<>();

        @Getter
        private final Promise<T> promise;

        /**
         * Create a new {@link Deferred} implementation.
         *
         * @param factory deferred factory
         */
        DeferredImpl(final DeferredFactory factory) {
            this.factory = factory;
            this.promise = new PromiseImpl<>(factory, this);
        }

        /**
         * Create a completed {@link Deferred} implementation using the given result and exception.
         *
         * @param factory deferred factory
         * @param result result
         * @param exception exception
         */
        DeferredImpl(
                final DeferredFactory factory,
                final Optional<T> result,
                final Optional<Exception> exception
        ) {
            this(factory);
            this.result = result;
            this.exception = exception;
        }

        /**
         * Check if a result is available.
         *
         * @return whether result is available
         */
        public synchronized boolean hasResult() {
            return this.result != null;
        }

        /**
         * Get result.
         *
         * @return result
         */
        public synchronized T getResult() {
            return this.result != null ? this.result.get() : null;
        }

        /**
         * Check if an exception is available.
         *
         * @return whether exception is available
         */
        public synchronized boolean hasException() {
            return this.exception != null;
        }

        /**
         * Get exception.
         *
         * @return exception
         */
        public synchronized Exception getException() {
            return this.exception != null ? this.exception.get() : null;
        }

        @Override
        public synchronized void resolve(final T actualResult) {
            if (!this.getPromise().isDone()) {
                this.result = Optional.ofNullable(actualResult);
                this.exception = null;
                for (final ConsumerCallback<T> handler : this.successHandlers) {
                    this.factory.defer(() -> handler.consume(actualResult));
                }
                this.notifyAll();
            }
        }

        @Override
        public synchronized void reject(final Exception actualException) {
            if (!this.getPromise().isDone()) {
                this.exception = Optional.ofNullable(actualException);
                this.result = null;
                if (!this.failHandlers.isEmpty()) {
                    for (final ConsumerCallback<Exception> handler : this.failHandlers) {
                        this.factory.defer(() -> handler.consume(actualException));
                    }
                }
                this.notifyAll();
            }
        }

        /**
         * Await completion.
         *
         * @param timeout timeout value
         * @param unit timeout unit
         * @return whether completed
         */
        private synchronized boolean await(final long timeout, final TimeUnit unit) {
            if (DeferredImpl.this.result == null) {
                try {
                    this.wait(unit.toMillis(timeout));
                    this.notifyAll();
                } catch (InterruptedException e) {
                    return false;
                }
            }
            return promise.isDone();
        }

    }

    /**
     * Implementation of {@link Promise}.
     *
     * @param <T> result type
     */
    private static class PromiseImpl<T> implements Promise<T> {

        private final DeferredFactory factory;
        private final DeferredImpl<T> deferred;

        /**
         * Create a new {@link Promise} implementation.
         *
         * @param factory deferred factory
         * @param deferred deferred
         */
        PromiseImpl(final DeferredFactory factory, final DeferredImpl deferred) {
            this.factory = factory;
            this.deferred = deferred;
        }

        @Override
        public <X> Promise<X> then(final ProcessorCallback<T, ?> success) {
            return this.createPromise(success, null);
        }

        @Override
        public <X> Promise<X> then(
                final ProcessorCallback<T, ?> success, final ProcessorCallback<Exception, ?> fail
        ) {
            return this.createPromise(success, fail);
        }

        @Override
        public <X> Promise<X> then(final ProcessorCallback<T, ?> success, final SupplierCallback<?> fail) {
            return this.createPromise(success, (c) -> fail);
        }

        @Override
        public Promise<Void> then(final ExecutorCallback success) {
            return this.createPromise(
                    (r) -> {
                        success.execute();
                        return null;
                    },
                    null
            );
        }

        @Override
        public Promise<Void> then() {
            return this.createPromise(
                    (r) -> null,
                    null
            );
        }

        @Override
        public Promise<T> fail(final T value) {
            return this.createPromise(
                    (r) -> r,
                    (e) -> value
            );
        }

        @Override
        public <X> Promise<X> fail(final SupplierCallback<?> callback) {
            return this.createPromise(
                    (r) -> r,
                    (e) -> callback.supply()
            );
        }

        @Override
        public Promise<Void> fail(final ConsumerCallback<Exception> callback) {
            return this.createPromise(
                    null,
                    (e) -> {
                        callback.consume(e);
                        return null;
                    }
            );
        }

        @Override
        public Promise<Void> fail(final ExecutorCallback callback) {
            return this.createPromise(
                    null,
                    (e) -> {
                        callback.execute();
                        return null;
                    }
            );
        }

        @Override
        public <X> Promise<X> always(final SupplierCallback<?> callback) {
            return this.createPromise(
                    (r) -> callback.supply(),
                    (e) -> callback.supply()
            );
        }

        @Override
        public Promise<Void> always(final ExecutorCallback callback) {
            return this.createPromise(
                    (r) -> {
                        callback.execute();
                        return null;
                    },
                    (e) -> {
                        callback.execute();
                        return null;
                    }
            );
        }

        @Override
        public Promise<Void> always() {
            return this.createPromise((r) -> null, (e) -> null);
        }

        @Override
        public boolean await() {
            return this.await(0, TimeUnit.MILLISECONDS);
        }

        @Override
        public boolean await(final long timeout, final TimeUnit unit) {
            return this.deferred.await(timeout, unit);
        }

        @Override
        public Promise<Void> apply(final Deferred target) {
            return this.createPromise((r) -> {
                target.resolve(r);
                return null;
            }, (e) -> {
                target.reject(e);
                return null;
            });
        }

        @Override
        public boolean isSuccess() {
            return this.deferred.hasResult();
        }

        @Override
        public boolean isFail() {
            return this.deferred.hasException();
        }

        @Override
        public boolean isDone() {
            return isSuccess() || isFail();
        }

        @Override
        public T get() {
            return this.get(0, TimeUnit.MILLISECONDS);
        }

        @Override
        public T get(final long timeout, final TimeUnit unit) {
            this.await(timeout, unit);
            return getNow();
        }

        @Override
        public T getNow() {
            if (this.deferred.hasException()) {
                throw new PromiseException(this.deferred.getException());
            } else {
                return this.deferred.getResult();
            }
        }

        /**
         * Create promise using callbacks.
         *
         * @param success success callback
         * @param fail fail callback
         * @param <X> promise result type
         * @return promise
         */
        private <X> Promise<X> createPromise(
                final ProcessorCallback<T, ?> success,
                final ProcessorCallback<Exception, ?> fail
        ) {
            final Deferred<X> result = this.factory.defer();

            if (success != null) {
                this.deferred.getSuccessHandlers().add((r) -> {
                    try {
                        this.resolve(result, success.process(r));
                    } catch (final Exception ex) {
                        result.reject(ex);
                    }
                });
            }

            if (fail != null) {
                this.deferred.getFailHandlers().add((e) -> {
                    try {
                        this.resolve(result, fail.process(e));
                    } catch (final Exception ex) {
                        result.reject(ex);
                    }
                });
            }

            return result.getPromise();
        }

        /**
         * Resolve deferred by value.
         *
         * @param target target deferred
         * @param value value
         * @param <X> deferred type
         */
        private <X> void resolve(final Deferred<X> target, final Object value) {
            if (value instanceof Promise) {
                Promise.class.cast(value).apply(target);
            } else {
                target.resolve((X) value);
            }
        }
    }
}
