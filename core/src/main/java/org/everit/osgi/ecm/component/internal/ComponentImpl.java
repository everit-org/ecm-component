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
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.ConfigurationException;

import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

public class ComponentImpl<C> {

    private final ComponentMetadata componentMetadata;

    private final Map<String, PropertyAttributeHelper<C, Object>> propertyAttributeHelpersByAttributeId =
            new HashMap<String, PropertyAttributeHelper<C, Object>>();

    private final AtomicReference<ComponentState> state = new AtomicReference<ComponentState>(ComponentState.STOPPED);

    private Object instance;

    private Throwable cause;

    private Thread processingThread;

    private final Class<C> componentType;

    private final BundleContext bundleContext;

    private final Method activateMethod;

    private Dictionary<String, Object> properties;

    public ComponentImpl(ComponentMetadata componentMetadata, BundleContext bundleContext) {
        this(componentMetadata, bundleContext, null);
    }

    public ComponentImpl(ComponentMetadata componentMetadata, BundleContext bundleContext,
            Dictionary<String, Object> properties) {
        this.componentMetadata = componentMetadata;
        this.bundleContext = bundleContext;
        this.properties = properties;

        Bundle bundle = bundleContext.getBundle();
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        ClassLoader classLoader = bundleWiring.getClassLoader();
        try {
            @SuppressWarnings("unchecked")
            Class<C> tmpComponentType = (Class<C>) classLoader.loadClass(componentMetadata.getType());
            componentType = tmpComponentType;
        } catch (ClassNotFoundException e) {
            // TODO
            throw new RuntimeException(e);
        }

        activateMethod = resolveActivateMethod();

        AttributeMetadata<?>[] attributes = componentMetadata.getAttributes();

        for (AttributeMetadata<?> attributeMetadata : attributes) {
            if (attributeMetadata instanceof PropertyAttributeMetadata) {

                @SuppressWarnings("unchecked")
                PropertyAttributeHelper<C, Object> propertyAttributeHelper =
                        new PropertyAttributeHelper<C, Object>(this,
                                (PropertyAttributeMetadata<Object>) attributeMetadata);

                propertyAttributeHelpersByAttributeId
                        .put(attributeMetadata.getAttributeId(), propertyAttributeHelper);
            } else {
                fillCapabilityCollectorsForReferenceAttributes((ReferenceMetadata) attributeMetadata);
            }
        }
    }

    private Method resolveActivateMethod() {
        String activateMethodName = componentMetadata.getActivateMethod();
        if (activateMethodName == null) {
            return null;
        }

        return null;
    }

    public Class<C> getComponentType() {
        return componentType;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    private void starting(Dictionary<String, Object> properties) {
        processingThread = Thread.currentThread();
        try {
            instance = componentType.newInstance();
        } catch (InstantiationException | IllegalAccessException | RuntimeException e) {
            fail(e, true);
            return;
        }

        Collection<PropertyAttributeHelper<C, Object>> propertyAttributeHelpers = propertyAttributeHelpersByAttributeId
                .values();

        try {
            for (PropertyAttributeHelper<C, Object> propertyAttributeHelper : propertyAttributeHelpers) {
                Object propertyValue = propertyAttributeHelper.resolveNewValue(properties);
                propertyAttributeHelper.applyValue(propertyValue);
            }
            callActivate();
        } catch (ConfigurationException | RuntimeException e) {
            fail(e, false);
            return;
        }
    }

    private void callActivate() {
        if (activateMethod == null) {
            return;
        }
        Parameter[] parameters = activateMethod.getParameters();

    }

    private void fail(Throwable e, boolean permanent) {
        cause = e;
        processingThread = null;
        if (permanent) {
            state.set(ComponentState.FAILED_PERMANENT);
        } else {
            state.set(ComponentState.FAILED);
        }
        // TODO Auto-generated catch block
        e.printStackTrace();
        return;
    }

    Object getInstance() {
        return instance;
    }

    public void close() {
        // TODO
    }

    private void fillCapabilityCollectorsForReferenceAttributes(ReferenceMetadata attributeMetadata) {
        // TODO Auto-generated method stub

    }

    public ComponentMetadata getComponentMetadata() {
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

        // TODO do it if all capabilities are satisfied.
        starting(properties);
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
