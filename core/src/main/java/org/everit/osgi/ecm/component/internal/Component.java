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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.ConfigurationException;

import org.everit.osgi.capabilitycollector.AbstractCapabilityCollector;
import org.everit.osgi.capabilitycollector.ServiceReferenceCollector;
import org.everit.osgi.ecm.component.context.ComponentContext;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;
import org.everit.osgi.ecm.metadata.ServiceReferenceMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

public class Component<C> implements ComponentContext<C> {

    private ActivateMethodHelper<C> activateMethodHelper;

    private final BundleContext bundleContext;

    private final List<AbstractCapabilityCollector<?>> capabilityCollectors =
            new ArrayList<AbstractCapabilityCollector<?>>();

    private Throwable cause;

    private final ComponentMetadata componentMetadata;

    private Class<C> componentType;

    private Object instance;

    private Thread processingThread;

    private Dictionary<String, Object> properties;

    private final Map<String, PropertyAttributeHelper<C, Object>> propertyAttributeHelpersByAttributeId =
            new HashMap<String, PropertyAttributeHelper<C, Object>>();

    final List<ServiceRegistration<?>> registeredServices = new ArrayList<ServiceRegistration<?>>();

    private final AtomicReference<ComponentState> state = new AtomicReference<ComponentState>(ComponentState.STOPPED);

    public Component(ComponentMetadata componentMetadata, BundleContext bundleContext) {
        this(componentMetadata, bundleContext, null);
    }

    public Component(ComponentMetadata componentMetadata, BundleContext bundleContext,
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
            fail(e, true);
            return;
        }

        activateMethodHelper = new ActivateMethodHelper<C>(componentMetadata, componentType);

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

    public void close() {
        // TODO
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

    private void fillCapabilityCollectorsForReferenceAttributes(ReferenceMetadata attributeMetadata) {
        // TODO Auto-generated method stub
        AbstractCapabilityCollector<?> collector;
        if (attributeMetadata instanceof ServiceReferenceMetadata) {
            ServiceReferenceMetadata serviceReferenceMetadata = (ServiceReferenceMetadata) attributeMetadata;
            collector = new ServiceReferenceCollector<?>(bundleContext, serviceReferenceMetadata.getServiceInterface(),
                    null, serviceReferenceMetadata.isDynamic(), actionHandler, false);
        } else {

        }
        capabilityCollectors.add(collector);
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public ComponentMetadata getComponentMetadata() {
        return componentMetadata;
    }

    @Override
    public ComponentRevision getComponentRevision() {
        // TODO
        return null;
    }

    public Class<C> getComponentType() {
        return componentType;
    }

    Object getInstance() {
        return instance;
    }

    @Override
    public Map<String, Object> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    public void open() {
        if (!state.compareAndSet(ComponentState.STOPPED, ComponentState.STARTING)) {
            throw new IllegalStateException("Component instance can be opened only when it is stopped.");
        }

        // TODO do it if all capabilities are satisfied.
        starting(properties);
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        ServiceRegistration<S> serviceRegistration = bundleContext.registerService(clazz, service, properties);
        return registerServiceInternal(serviceRegistration);
    }

    @Override
    public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(clazz, service, properties);
        return registerServiceInternal(serviceRegistration);
    }

    @Override
    public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(clazzes, service, properties);
        return registerServiceInternal(serviceRegistration);
    }

    private <S> ServiceRegistration<S> registerServiceInternal(ServiceRegistration<S> original) {
        ComponentServiceRegistration<S, C> componentServiceRegistration = new ComponentServiceRegistration<S, C>(
                this, original);
        registeredServices.add(componentServiceRegistration);
        return componentServiceRegistration;
    }

    /**
     * Called when when the target of a non-dynamic reference should be replaced).
     */
    void restart() {
        // TODO
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
            activateMethodHelper.call(this, instance);
        } catch (ConfigurationException | RuntimeException e) {
            fail(e, false);
            return;
        } catch (IllegalAccessException e) {
            fail(e, true);
            return;
        } catch (InvocationTargetException e) {
            fail(e.getCause(), false);
        }
    }

    public void updateConfiguration(Dictionary<String, ?> properties) {
        // TODO Auto-generated method stub

    }
}
