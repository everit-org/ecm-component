/**
 * This file is part of Everit - ECM Component RI.
 *
 * Everit - ECM Component RI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - ECM Component RI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - ECM Component RI.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.ecm.component.ri.internal;

import java.util.List;
import java.util.Map;

import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class ComponentRevisionImpl implements ComponentRevision {

    public static class Builder {

        private Throwable cause = null;

        private Thread processingThread = null;

        private Map<String, Object> properties;

        private ComponentState state = ComponentState.STOPPED;

        public Builder(final Map<String, Object> properties) {
            this.properties = properties;
        }

        public synchronized void active() {
            this.state = ComponentState.ACTIVE;
            this.processingThread = null;

        }

        public synchronized ComponentRevisionImpl build() {
            return new ComponentRevisionImpl(this);
        }

        public synchronized void fail(final Throwable cause, final boolean permanent) {
            this.cause = cause;
            this.processingThread = null;
            if (permanent) {
                this.state = ComponentState.FAILED_PERMANENT;
            } else {
                this.state = ComponentState.FAILED;
            }
        }

        public Map<String, Object> getProperties() {
            return properties;
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
            if (targetState != ComponentState.UPDATING_CONFIGURATION) {
                this.processingThread = null;
            }
            if (targetState == ComponentState.STOPPED || targetState == ComponentState.UPDATING_CONFIGURATION) {
                cause = null;
            }
            state = targetState;
        }

        public synchronized void stopping() {
            this.state = ComponentState.STOPPING;
            this.processingThread = Thread.currentThread();
        }

        public synchronized void updateProperties(final Map<String, Object> properties) {
            this.properties = properties;
            this.state = ComponentState.UPDATING_CONFIGURATION;
        }
    }

    private final Throwable cause;

    private final Thread processingThread;

    private final Map<String, Object> properties;

    private final ComponentState state;

    private ComponentRevisionImpl(final Builder builder) {
        this.state = builder.state;
        this.processingThread = builder.processingThread;
        this.cause = builder.cause;
        this.properties = builder.properties;
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
    public Map<String, Object> getProperties() {
        return properties;
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
