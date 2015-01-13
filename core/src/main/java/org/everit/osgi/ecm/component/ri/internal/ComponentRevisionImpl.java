package org.everit.osgi.ecm.component.ri.internal;

import java.util.Dictionary;
import java.util.List;

import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class ComponentRevisionImpl implements ComponentRevision {

    public static class Builder {

        private ComponentState state;

        public ComponentRevisionImpl build() {
            return new ComponentRevisionImpl(this);
        }

        public void setState(final ComponentState state) {
            this.state = state;
        }
    }

    private final ComponentState state;

    private ComponentRevisionImpl(final Builder builder) {
        this.state = builder.state;
    }

    @Override
    public List<Capability> getCapabilities(final String namespace) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Throwable getCause() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Thread getProcessingThread() {
        // TODO Auto-generated method stub
        return null;
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
