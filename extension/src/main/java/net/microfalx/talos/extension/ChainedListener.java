package net.microfalx.talos.extension;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;

import java.util.concurrent.CopyOnWriteArrayList;

class ChainedListener extends AbstractExecutionListener {

    private final ExecutionListener delegate;

    private final CopyOnWriteArrayList<ExecutionListener> chainedListeners = new CopyOnWriteArrayList<>();

    ChainedListener(ExecutionListener delegate) {
        this.delegate = delegate;
    }

    void addChainListener(ExecutionListener listener) {
        chainedListeners.addIfAbsent(listener);
    }

    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        delegate.projectDiscoveryStarted(event);
        chainedListeners.forEach(listener -> listener.projectDiscoveryStarted(event));
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        delegate.sessionStarted(event);
        chainedListeners.forEach(listener -> listener.sessionStarted(event));
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        delegate.sessionEnded(event);
        chainedListeners.forEach(listener -> listener.sessionEnded(event));
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        delegate.projectSkipped(event);
        chainedListeners.forEach(listener -> listener.projectSkipped(event));
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        delegate.projectStarted(event);
        chainedListeners.forEach(listener -> listener.projectStarted(event));
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        delegate.projectSucceeded(event);
        chainedListeners.forEach(listener -> listener.projectSucceeded(event));
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        delegate.projectFailed(event);
        chainedListeners.forEach(listener -> listener.projectFailed(event));
    }

    @Override
    public void forkStarted(ExecutionEvent event) {
        delegate.forkStarted(event);
        chainedListeners.forEach(listener -> listener.forkStarted(event));
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        delegate.forkSucceeded(event);
        chainedListeners.forEach(listener -> listener.forkSucceeded(event));
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        delegate.forkFailed(event);
        chainedListeners.forEach(listener -> listener.forkFailed(event));
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        delegate.mojoSkipped(event);
        chainedListeners.forEach(listener -> listener.mojoSkipped(event));
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        delegate.mojoStarted(event);
        chainedListeners.forEach(listener -> listener.mojoStarted(event));
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        delegate.mojoSucceeded(event);
        chainedListeners.forEach(listener -> listener.mojoSucceeded(event));
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        delegate.mojoFailed(event);
        chainedListeners.forEach(listener -> listener.mojoFailed(event));
    }

    @Override
    public void forkedProjectStarted(ExecutionEvent event) {
        delegate.forkedProjectStarted(event);
        chainedListeners.forEach(listener -> listener.forkedProjectStarted(event));
    }

    @Override
    public void forkedProjectSucceeded(ExecutionEvent event) {
        delegate.forkedProjectSucceeded(event);
        chainedListeners.forEach(listener -> listener.forkedProjectSucceeded(event));
    }

    @Override
    public void forkedProjectFailed(ExecutionEvent event) {
        delegate.forkedProjectFailed(event);
        chainedListeners.forEach(listener -> listener.forkedProjectFailed(event));
    }
}
