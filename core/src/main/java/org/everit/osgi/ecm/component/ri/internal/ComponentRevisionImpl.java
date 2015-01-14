package org.everit.osgi.ecm.component.ri.internal;

import java.util.Dictionary;
import java.util.List;

import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class ComponentRevisionImpl implements ComponentRevision {

    public static class Builder {

        private Throwable cause = null;

        private Thread processingThread = null;

        private ComponentState state = ComponentState.STOPPED;

        public synchronized ComponentRevisionImpl build() {
            return new ComponentRevisionImpl(this);
        }

        public synchronized void fail(final Throwable cause, final boolean permanent) {
            this.cause = cause;
            if (permanent) {
                this.state = ComponentState.FAILED_PERMANENT;
            } else {
                this.state = ComponentState.FAILED;
            }
        }

        public ComponentState getState() {
            return state;
        }

        public synchronized void starting() {
            this.state = ComponentState.STARTING;
            this.cause = null;
            this.processingThread = Thread.currentThread();
        }

        public synchronized void stopped(final ComponentState targetState) {
            this.processingThread = null;
            if (targetState == ComponentState.STOPPED) {
                cause = null;
            }
            state = targetState;
        }

        public synchronized void stopping() {
            this.state = ComponentState.STOPPING;
            this.processingThread = Thread.currentThread();
        }
    }

    private final Throwable cause;

    private final Thread processingThread;

    private final ComponentState state;

    private ComponentRevisionImpl(final Builder builder) {
        this.state = builder.state;
        this.processingThread = builder.processingThread;
        this.cause = builder.cause;
    }

    @Override
    public List<Capability> getCapabilities(final String namespace) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public Thread getProcessingThread() {
        return processingThread;
    }

    @Override
    public Dictionary<String, ?> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Requirement> getRequirements(final String namespace) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ComponentState getState() {
        return state;
    }

}
