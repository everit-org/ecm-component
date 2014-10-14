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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.ConfigurationException;

import org.everit.osgi.ecm.component.context.ComponentContext;
import org.everit.osgi.ecm.component.internal.attribute.BundleCapabilityReferenceAttributeHelper;
import org.everit.osgi.ecm.component.internal.attribute.PropertyAttributeHelper;
import org.everit.osgi.ecm.component.internal.attribute.ReferenceHelper;
import org.everit.osgi.ecm.component.internal.attribute.ServiceReferenceAttributeHelper;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.BundleCapabilityReferenceMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;
import org.everit.osgi.ecm.metadata.ServiceMetadata;
import org.everit.osgi.ecm.metadata.ServiceReferenceMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

public class ComponentContextImpl<C> implements ComponentContext<C> {

    private class ReferenceEventHandlerImpl implements ReferenceEventHandler {

        private int satisfiedCapabilities = 0;

        @Override
        public synchronized void satisfied() {
            satisfiedCapabilities++;

            if (satisfiedCapabilities == referenceHelpers.size() && state.get() == ComponentState.UNSATISFIED) {
                starting();
            }
        }

        @Override
        public synchronized void unsatisfied() {
            satisfiedCapabilities--;

            if (state.get() == ComponentState.ACTIVE) {
                stopping();
            }
        }

    }

    private static void resolveSuperInterfacesRecurse(Class<?> currentClass, Set<String> interfaces) {
        Class<?>[] superInterfaces = currentClass.getInterfaces();
        for (Class<?> superInterface : superInterfaces) {
            interfaces.add(superInterface.getName());
            resolveSuperInterfacesRecurse(superInterface, interfaces);
        }
    }

    private ActivateMethodHelper<C> activateMethodHelper;

    private final BundleContext bundleContext;

    private Throwable cause;

    private final ComponentMetadata componentMetadata;

    private Class<C> componentType;

    private C instance;

    private volatile Thread processingThread;

    private volatile Dictionary<String, Object> properties;

    private final Map<String, PropertyAttributeHelper<C, Object>> propertyAttributeHelpersByAttributeId =
            new HashMap<String, PropertyAttributeHelper<C, Object>>();

    private final ReferenceEventHandler referenceEventHandler = new ReferenceEventHandlerImpl();

    private final List<ReferenceHelper<?, C>> referenceHelpers =
            new ArrayList<ReferenceHelper<?, C>>();

    final List<ServiceRegistration<?>> registeredServices = new ArrayList<ServiceRegistration<?>>();

    private String[] serviceInterfaces;

    private ServiceRegistration<?> serviceRegistration = null;

    private final AtomicReference<ComponentState> state = new AtomicReference<ComponentState>(ComponentState.STOPPED);

    public ComponentContextImpl(ComponentMetadata componentMetadata, BundleContext bundleContext) {
        this(componentMetadata, bundleContext, null);
    }

    public ComponentContextImpl(ComponentMetadata componentMetadata, BundleContext bundleContext,
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

        serviceInterfaces = resolveServiceInterfaces();

    }

    public void close() {
        if (referenceHelpers.size() == 0) {
            stopping();
        } else {
            for (ReferenceHelper<?, C> referenceHelper : referenceHelpers) {
                referenceHelper.close();
            }
        }
    }

    @Override
    public void fail(Throwable e, boolean permanent) {
        cause = e;
        unregisterServices();
        processingThread = null;
        instance = null;
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
        ReferenceHelper<?, C> helper;
        if (attributeMetadata instanceof ServiceReferenceMetadata) {
            helper = new ServiceReferenceAttributeHelper<Object, C>((ServiceReferenceMetadata) attributeMetadata,
                    this, null);
        } else {
            helper = new BundleCapabilityReferenceAttributeHelper<C>(
                    (BundleCapabilityReferenceMetadata) attributeMetadata, this, null);
        }
        referenceHelpers.add(helper);
        helper.open();
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public ComponentMetadata getComponentMetadata() {
        return componentMetadata;
    }

    @Override
    public ComponentRevision getComponentRevision() {
        // TODO
        return null;
    }

    @Override
    public Class<C> getComponentType() {
        return componentType;
    }

    @Override
    public C getInstance() {
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
        if (referenceHelpers.size() == 0) {
            starting();
        } else {
            state.set(ComponentState.UNSATISFIED);
            for (Iterator<ReferenceHelper<?, C>> iterator = referenceHelpers.iterator(); iterator.hasNext()
                    && state.get() == ComponentState.UNSATISFIED;) {
                ReferenceHelper<?, C> referenceAttributeHelper = iterator.next();
                referenceAttributeHelper.open();
            }
        }
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        validateComponentStateForServiceRegistration();
        ServiceRegistration<S> serviceRegistration = bundleContext.registerService(clazz, service, properties);
        return registerServiceInternal(serviceRegistration);
    }

    @Override
    public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
        validateComponentStateForServiceRegistration();
        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(clazz, service, properties);
        return registerServiceInternal(serviceRegistration);
    }

    @Override
    public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
        validateComponentStateForServiceRegistration();
        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(clazzes, service, properties);
        return registerServiceInternal(serviceRegistration);
    }

    private <S> ServiceRegistration<S> registerServiceInternal(ServiceRegistration<S> original) {
        ComponentServiceRegistration<S, C> componentServiceRegistration = new ComponentServiceRegistration<S, C>(
                this, original);
        registeredServices.add(componentServiceRegistration);
        return componentServiceRegistration;
    }

    private String[] resolveServiceInterfaces() {
        ServiceMetadata serviceMetadata = componentMetadata.getService();
        if (serviceMetadata == null) {
            return null;
        }

        Class<?>[] clazzes = serviceMetadata.getClazzes();
        if (clazzes.length > 0) {
            String[] result = new String[clazzes.length];
            for (int i = 0; i < clazzes.length; i++) {
                result[i] = clazzes[i].getName();
            }
            return result;
        }

        // Auto detect
        Set<String> interfaces = new HashSet<String>();
        Class<?> currentClass = componentType;
        resolveSuperInterfacesRecurse(currentClass, interfaces);

        if (interfaces.size() != 0) {
            return interfaces.toArray(new String[interfaces.size()]);
        }
        return new String[] { componentType.getName() };
    }

    @Override
    public void restart() {
        // TODO
    }

    private void starting() {
        state.set(ComponentState.STARTING);
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
            return;
        }

        if (serviceInterfaces != null) {
            serviceRegistration = registerService(serviceInterfaces, instance, properties);
        }
    }

    private void stopping() {
        state.set(ComponentState.STOPPING);
        instance = null;
        unregisterServices();

        state.set(ComponentState.STOPPED);

    }

    private void unregisterServices() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
        for (Iterator<ServiceRegistration<?>> iterator = registeredServices.iterator(); iterator
                .hasNext();) {
            ServiceRegistration<?> serviceRegistration = iterator.next();
            // TODO WARN the user that the code is not stable as the services should have been unregistered at this
            // point.
            serviceRegistration.unregister();
            iterator.remove();
        }

    }

    public void updateConfiguration(Dictionary<String, Object> properties) {
        this.properties = properties;
        if (state.get() == ComponentState.FAILED) {
            starting();
        }
        // TODO Auto-generated method stub

    }

    private void validateComponentStateForServiceRegistration() {
        ComponentState componentState = state.get();
        if (componentState != ComponentState.ACTIVE && componentState != ComponentState.STARTING) {
            throw new IllegalStateException(
                    "Service can only be registered in component if the state of the component is ACTIVE or STARTING");
        }
    }
}
