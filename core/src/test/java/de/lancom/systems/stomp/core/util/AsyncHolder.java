package de.lancom.systems.stomp.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AsyncHolder<T> {

    private final List<T> values = new ArrayList<T>();

    public static <T> AsyncHolder<T> create() {
        return new AsyncHolder<T>();
    }

    private AsyncHolder() {
    }

    public synchronized void set(final T value) {
        this.values.add(value);
        this.notifyAll();
    }

    public synchronized T get() {
        return get(Math.max(values.size(), 1));
    }

    public synchronized T get(final int position) {
        if (position < 1 || position > values.size()) {
            return null;
        } else {
            return values.get(position - 1);
        }
    }

    public boolean isSet() {
        return values.size() > 0;
    }

    public boolean isSet(final int position) {
        return position > 0 && position <= values.size();
    }

    public int getCount() {
        return values.size();
    }

    public void clear() {
        this.values.clear();
    }

    public synchronized T get(final int timeout, final TimeUnit unit) {
        return get(Math.max(values.size(), 1), timeout, unit);
    }

    public synchronized T get(final int position, final int timeout, final TimeUnit unit) {
        try {
            final CountDown countDown = new CountDown(timeout, unit);
            while (countDown.remaining() > 0 && position > values.size()) {
                final long start = System.currentTimeMillis();
                this.wait(countDown.remaining());
            }
            return get(position);
        } catch (final InterruptedException ex) {
            return null;
        }
    }

}
