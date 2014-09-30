/**
 * This file is part of Everit - ECM Component.
 *
 * Everit - ECM Component is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - ECM Component is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - ECM Component.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.ecm.component.internal;

import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicReference;

import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;

public class ComponentImpl<C> {

    private final ComponentMetadata<C> componentMetadata;

    private final AtomicReference<ComponentState> state = new AtomicReference<ComponentState>(ComponentState.STOPPED);

    public ComponentImpl(ComponentMetadata<C> componentMetadata) {
        this(componentMetadata, null);
    }

    public ComponentImpl(ComponentMetadata<C> componentMetadata, Dictionary<String, ?> properties) {
        this.componentMetadata = componentMetadata;
        AttributeMetadata<?>[] attributes = componentMetadata.getAttributes();
        for (AttributeMetadata<?> attributeMetadata : attributes) {
        }
        // TODO Auto-generated constructor stub
    }

    public void close() {
        // TODO
    }

    public ComponentRevision getComponentRevision() {
        // TODO
        return null;
    }

    public void open() {
        if (!state.compareAndSet(ComponentState.STOPPED, ComponentState.STARTING)) {
            throw new IllegalStateException("Component instance can be opened only when it is stopped.");
        }

        // TODO
    }

    public void updateConfiguration(Dictionary<String, ?> properties) {
        // TODO Auto-generated method stub

    }
}
