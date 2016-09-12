package de.lancom.systems.stomp.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for asynchronous data.
 *
 * @param <T> data type
 */
public final class AsyncHolder<T> {

    private final List<T> values = new ArrayList<T>();

    /**
     * Create a new holder.
     *
     * @param <T> data type
     * @return holder
     */
    public static <T> AsyncHolder<T> create() {
        return new AsyncHolder<T>();
    }

    /**
     * Private constructor.
     */
    private AsyncHolder() {
        super();
    }

    /**
     * Set value and notify waiting parties.
     *
     * @param value value
     */
    public synchronized void set(final T value) {
        this.values.add(value);
        this.notifyAll();
    }

    /**
     * Get value at last position directly.
     *
     * @return value
     */
    public synchronized T get() {
        return get(Math.max(values.size(), 1));
    }

    /**
     * Get value at given position directly.
     *
     * @param position position
     * @return value
     */
    public synchronized T get(final int position) {
        if (position < 1 || position > values.size()) {
            return null;
        } else {
            return values.get(position - 1);
        }
    }

    /**
     * Check if holder has any value.
     *
     * @return true if value is available
     */
    public boolean isSet() {
        return values.size() > 0;
    }

    /**
     * Check if holder has any value at the given position.
     *
     * @param position position
     * @return true if value at the given position is available
     */
    public boolean isSet(final int position) {
        return position > 0 && position <= values.size();
    }

    /**
     * Get count of avalailable values.
     *
     * @return count
     */
    public int getCount() {
        return values.size();
    }

    /**
     * Remove all values.
     */
    public void clear() {
        this.values.clear();
    }

    /**
     * Get value at the last position or wait until timeout.
     *
     * @param timeout timeout value
     * @param unit timout unit
     * @return value
     */
    public synchronized T get(final int timeout, final TimeUnit unit) {
        return get(Math.max(values.size(), 1), timeout, unit);
    }

    /**
     * Get value at the given position or wait until timeout.
     *
     * @param position position
     * @param timeout timeout value
     * @param unit timout unit
     * @return value
     */
    public synchronized T get(final int position, final int timeout, final TimeUnit unit) {
        expect(position, timeout, unit);
        return get(position);
    }

    /**
     * Wait for the given entry.
     *
     * @param count count
     * @param timeout timeout value
     * @param unit timout unit
     */
    public synchronized void expect(final int count, final int timeout, final TimeUnit unit) {
        try {
            final long start = System.currentTimeMillis();
            while (count > values.size()) {
                final long remaining = System.currentTimeMillis() - start + unit.toMillis(timeout);
                if (remaining > 0) {
                    this.wait(remaining);
                } else {
                    break;
                }
            }
        } catch (final InterruptedException ex) {
            return;
        }
    }

}
