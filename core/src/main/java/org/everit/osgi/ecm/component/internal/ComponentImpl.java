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

import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;

public class ComponentImpl<C> {

    private final ComponentMetadata<C> componentMetadata;

    private final Map<String, Method> settersOfPropertyAttributes = new HashMap<String, Method>();

    private final AtomicReference<ComponentState> state = new AtomicReference<ComponentState>(ComponentState.STOPPED);

    public ComponentImpl(ComponentMetadata<C> componentMetadata) {
        this(componentMetadata, null);
    }

    public ComponentImpl(ComponentMetadata<C> componentMetadata, Dictionary<String, ?> properties) {
        this.componentMetadata = componentMetadata;
        AttributeMetadata<?>[] attributes = componentMetadata.getAttributes();
        for (AttributeMetadata<?> attributeMetadata : attributes) {
            if (attributeMetadata instanceof PropertyAttributeMetadata) {
                fillSettersForPropertyAttributes((PropertyAttributeMetadata<?>) attributeMetadata);
            } else {
                fillCapabilityCollectorsForReferenceAttributes((ReferenceMetadata) attributeMetadata);
            }
        }

    }

    public void close() {
        // TODO
    }

    private void fillCapabilityCollectorsForReferenceAttributes(ReferenceMetadata attributeMetadata) {
        // TODO Auto-generated method stub

    }

    private void fillSettersForPropertyAttributes(PropertyAttributeMetadata<?> attributeMetadata) {
        // TODO Auto-generated method stub

    }

    public ComponentMetadata<C> getComponentMetadata() {
        return componentMetadata;
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

    /**
     * Called when when the target of a non-dynamic reference should be replaced).
     */
    void restart() {
        // TODO
    }

    public void updateConfiguration(Dictionary<String, ?> properties) {
        // TODO Auto-generated method stub

    }
}
