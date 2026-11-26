package io.github.schlafferbenjamin.javaq.internal;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AbstractIterator<T> implements Iterator<T> {
    private enum State { READY, NOT_READY, DONE, FAILED };

    private State state = State.NOT_READY;
    private T next;

    protected abstract T computeNext();

    protected final T endOfData() {
        state= State.DONE;
        return null;
    }

    @Override
    public final boolean hasNext() {
        if (state == State.FAILED) {
            throw new IllegalStateException("Iterator is in a failed state");
        }
        return switch (state) {
            case DONE -> false;
            case READY -> true;
            default -> tryToComputeNext();
        };
    }

    private boolean tryToComputeNext() {
        state = State.FAILED;
        next = computeNext();

        if (state == State.DONE) return false;

        state = State.READY;
        return true;
    };


    @Override
    public final T next() {
        if (!hasNext()) throw new NoSuchElementException();

        state = State.NOT_READY;
        T result = next;
        next = null;
        return result;
    }
}

