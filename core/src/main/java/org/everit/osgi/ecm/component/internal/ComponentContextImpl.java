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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
import org.everit.osgi.ecm.metadata.ServiceMetadata;
import org.everit.osgi.ecm.metadata.ServiceReferenceMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

public class ComponentContextImpl<C> implements ComponentContext<C> {

    private class ReferenceEventHandlerImpl implements ReferenceEventHandler {

        private int satisfiedCapabilities = 0;

        @Override
        public void changedNonDynamic() {
            Lock writeLock = readWriteLock.writeLock();
            writeLock.lock();
            try {
                if (state == ComponentState.ACTIVE) {
                    restart();
                }
            } finally {
                writeLock.unlock();
            }

        }

        @Override
        public synchronized void satisfied() {
            Lock writeLock = readWriteLock.writeLock();
            writeLock.lock();
            try {
                satisfiedCapabilities++;

                if (satisfiedCapabilities == referenceHelpers.size() && state == ComponentState.UNSATISFIED) {
                    // TODO do we want to start if failed?
                    starting();
                }
            } finally {
                writeLock.unlock();
            }
        }

        @Override
        public synchronized void unsatisfied() {
            Lock writeLock = readWriteLock.writeLock();
            writeLock.lock();
            try {
                satisfiedCapabilities--;

                if (state == ComponentState.ACTIVE) {
                    // TODO do we want to stop if failed?
                    stopping();
                }
            } finally {
                writeLock.unlock();
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

    private Method deactivateMethod;

    private C instance;

    private boolean opened = false;

    private volatile Thread processingThread;

    private volatile PropertiesHolder properties;

    private final Map<String, PropertyAttributeHelper<C, Object>> propertyAttributeHelpersByAttributeId =
            new HashMap<String, PropertyAttributeHelper<C, Object>>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final ReferenceEventHandler referenceEventHandler = new ReferenceEventHandlerImpl();

    private final List<ReferenceHelper<?, C>> referenceHelpers =
            new ArrayList<ReferenceHelper<?, C>>();

    final List<ServiceRegistration<?>> registeredServices = new ArrayList<ServiceRegistration<?>>();

    private String[] serviceInterfaces;

    private ServiceRegistration<?> serviceRegistration = null;

    private volatile ComponentState state = ComponentState.STOPPED;

    public ComponentContextImpl(ComponentMetadata componentMetadata, BundleContext bundleContext) {
        this(componentMetadata, bundleContext, null);
    }

    public ComponentContextImpl(ComponentMetadata componentMetadata, BundleContext bundleContext,
            Dictionary<String, Object> properties) {
        this.componentMetadata = componentMetadata;
        this.bundleContext = bundleContext;
        this.properties = new PropertiesHolder(properties);

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

        fillAttributeHelpers(attributes);

        serviceInterfaces = resolveServiceInterfaces();

        deactivateMethod = resolveDeactivateMethod();

    }

    public void close() {
        Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            if (!opened) {
                throw new IllegalStateException("Cannot close a component context that is not opened");
            }
            opened = false;
            if (referenceHelpers.size() == 0) {
                stopping();
            } else {
                for (ReferenceHelper<?, C> referenceHelper : referenceHelpers) {
                    referenceHelper.close();
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void fail(Throwable e, boolean permanent) {
        cause = e;
        unregisterServices();
        processingThread = null;
        instance = null;
        if (permanent) {
            state = ComponentState.FAILED_PERMANENT;
        } else {
            state = ComponentState.FAILED;
        }
        // TODO Auto-generated catch block
        e.printStackTrace();
        return;
    }

    private void fillAttributeHelpers(AttributeMetadata<?>[] attributes) {
        for (AttributeMetadata<?> attributeMetadata : attributes) {
            if (attributeMetadata instanceof PropertyAttributeMetadata) {

                @SuppressWarnings("unchecked")
                PropertyAttributeHelper<C, Object> propertyAttributeHelper =
                        new PropertyAttributeHelper<C, Object>(this,
                                (PropertyAttributeMetadata<Object>) attributeMetadata);

                propertyAttributeHelpersByAttributeId
                        .put(attributeMetadata.getAttributeId(), propertyAttributeHelper);
            } else {
                ReferenceHelper<?, C> helper;
                if (attributeMetadata instanceof ServiceReferenceMetadata) {
                    helper = new ServiceReferenceAttributeHelper<Object, C>(
                            (ServiceReferenceMetadata) attributeMetadata,
                            this, referenceEventHandler);
                } else {
                    helper = new BundleCapabilityReferenceAttributeHelper<C>(
                            (BundleCapabilityReferenceMetadata) attributeMetadata, this, referenceEventHandler);
                }
                referenceHelpers.add(helper);
            }
        }
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
        return properties.getMap();
    }

    private boolean isFailed() {
        return ComponentState.FAILED == state || ComponentState.FAILED_PERMANENT == state;
    }

    public void open() {
        Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();

        try {
            if (opened) {
                throw new IllegalStateException("Cannot open a component context that is already opened");
            }
            opened = true;
            if (referenceHelpers.size() == 0) {
                starting();
            } else {
                state = ComponentState.UNSATISFIED;
                for (Iterator<ReferenceHelper<?, C>> iterator = referenceHelpers.iterator(); iterator.hasNext()
                        && state == ComponentState.UNSATISFIED;) {
                    ReferenceHelper<?, C> referenceAttributeHelper = iterator.next();
                    referenceAttributeHelper.open();
                }
            }
        } finally {
            writeLock.unlock();
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

    private Method resolveDeactivateMethod() {
        MethodDescriptor methodDescriptor = componentMetadata.getDeactivate();
        if (methodDescriptor == null) {
            return null;
        }
        Method method = methodDescriptor.locate(componentType, false);
        if (method == null) {
            Exception e = new IllegalMetadataException("Could not find method '" + methodDescriptor.toString()
                    + "' for type " + componentType);
            fail(e, true);
        }
        if (method.getParameterTypes().length > 0) {
            Exception e = new IllegalMetadataException("Deactivate method must not have any parameters. Method '"
                    + method.toGenericString() + "' of type " + componentType + " does have.");
            fail(e, true);
        }
        return method;
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
        Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            if (state != ComponentState.ACTIVE && state != ComponentState.FAILED) {
                throw new IllegalStateException(
                        "Only ACTIVE and FAILED components can be restarted, while the state of the component "
                                + componentMetadata.getComponentId() + " is " + state.toString());
            }
            stopping();
            starting();
        } finally {
            writeLock.unlock();
        }
        // TODO
    }

    private void starting() {
        Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        if (state == ComponentState.FAILED_PERMANENT) {
            return;
        }
        try {
            state = ComponentState.STARTING;
            processingThread = Thread.currentThread();
            try {
                instance = componentType.newInstance();
            } catch (InstantiationException | IllegalAccessException | RuntimeException e) {
                fail(e, true);
                return;
            }

            for (ReferenceHelper<?, C> referenceHelper : referenceHelpers) {
                referenceHelper.bind();
            }

            if (isFailed()) {
                return;
            }

            Collection<PropertyAttributeHelper<C, Object>> propertyAttributeHelpers = propertyAttributeHelpersByAttributeId
                    .values();

            try {
                for (PropertyAttributeHelper<C, Object> propertyAttributeHelper : propertyAttributeHelpers) {
                    Object propertyValue = propertyAttributeHelper.resolveNewValue(properties.getDictionary());
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
                serviceRegistration = registerService(serviceInterfaces, instance, properties.getDictionary());
            }
            state = ComponentState.ACTIVE;
        } finally {
            processingThread = null;
            writeLock.unlock();
        }
    }

    private void stopping() {
        Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            state = ComponentState.STOPPING;
            processingThread = Thread.currentThread();
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
            if (deactivateMethod != null) {
                try {
                    deactivateMethod.invoke(instance);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            unregisterServices();
            instance = null;

            state = ComponentState.STOPPED;
        } finally {
            processingThread = null;
            writeLock.unlock();
        }
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
        Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            this.properties = new PropertiesHolder(properties);
            if (state == ComponentState.FAILED_PERMANENT) {
                return;
            }
            if (state == ComponentState.FAILED) {
                restart();
                return;
            }
            if (state == ComponentState.ACTIVE) {
                // TODO check if restart is necessary.
                stopping();
                starting();
                return;
            }
            fail(new IllegalStateException(
                    "Updating component configuration is only possible if the component is in FAILED or ACTIVE state"),
                    false);
        } finally {
            writeLock.unlock();
        }
        // TODO Auto-generated method stub

    }

    private void validateComponentStateForServiceRegistration() {
        if (state != ComponentState.ACTIVE && state != ComponentState.STARTING) {
            throw new IllegalStateException(
                    "Service can only be registered in component if the state of the component is ACTIVE or STARTING");
        }
    }
}
