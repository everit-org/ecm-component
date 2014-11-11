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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.ConfigurationException;

import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.component.ri.internal.attribute.BundleCapabilityReferenceAttributeHelper;
import org.everit.osgi.ecm.component.ri.internal.attribute.PropertyAttributeHelper;
import org.everit.osgi.ecm.component.ri.internal.attribute.ReferenceHelper;
import org.everit.osgi.ecm.component.ri.internal.attribute.ServiceReferenceAttributeHelper;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.BundleCapabilityReferenceMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.metadata.ServiceMetadata;
import org.everit.osgi.ecm.metadata.ServiceReferenceMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

public class ComponentContextImpl<C> implements ComponentContext<C> {

    private class ReferenceEventHandlerImpl implements ReferenceEventHandler {

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
                    stopping(ComponentState.UNSATISFIED);
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

    private volatile Map<String, Object> properties;

    private final List<PropertyAttributeHelper<C, Object>> propertyAttributeHelpers = new ArrayList<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final ReferenceEventHandler referenceEventHandler = new ReferenceEventHandlerImpl();

    private final List<ReferenceHelper<?, C, ?>> referenceHelpers = new ArrayList<>();

    final List<ServiceRegistration<?>> registeredServices = new ArrayList<ServiceRegistration<?>>();

    private int satisfiedCapabilities = 0;

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
        this.properties = resolveProperties(properties);

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
            if (state == ComponentState.ACTIVE) {
                stopping(ComponentState.STOPPED);
            } else {
                for (ReferenceHelper<?, C, ?> referenceHelper : referenceHelpers) {
                    referenceHelper.close();
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    private boolean equals(Object oldValue, Object newValue) {
        if ((oldValue == null && newValue != null) || (oldValue != null && newValue == null)) {
            return false;
        }
        if (oldValue != null && !oldValue.equals(newValue)) {

            Class<? extends Object> oldValueClass = oldValue.getClass();
            Class<? extends Object> newValueClass = newValue.getClass();
            if (!oldValueClass.equals(newValueClass) || !oldValueClass.isArray()) {
                return false;
            }

            boolean equals;
            if (oldValueClass.equals(boolean[].class)) {
                equals = Arrays.equals((boolean[]) oldValue, (boolean[]) newValue);
            } else if (oldValueClass.equals(byte[].class)) {
                equals = Arrays.equals((byte[]) oldValue, (byte[]) newValue);
            } else if (oldValueClass.equals(char[].class)) {
                equals = Arrays.equals((char[]) oldValue, (char[]) newValue);
            } else if (oldValueClass.equals(double[].class)) {
                equals = Arrays.equals((double[]) oldValue, (double[]) newValue);
            } else if (oldValueClass.equals(float[].class)) {
                equals = Arrays.equals((float[]) oldValue, (float[]) newValue);
            } else if (oldValueClass.equals(int[].class)) {
                equals = Arrays.equals((int[]) oldValue, (int[]) newValue);
            } else if (oldValueClass.equals(long[].class)) {
                equals = Arrays.equals((long[]) oldValue, (long[]) newValue);
            } else if (oldValueClass.equals(short[].class)) {
                equals = Arrays.equals((short[]) oldValue, (short[]) newValue);
            } else {
                equals = Arrays.equals((Object[]) oldValue, (Object[]) newValue);
            }

            if (!equals) {
                return false;
            }
        }
        return true;
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

                propertyAttributeHelpers
                        .add(propertyAttributeHelper);
            } else {
                ReferenceHelper<?, C, ?> helper;
                try {
                    if (attributeMetadata instanceof ServiceReferenceMetadata) {

                        helper = new ServiceReferenceAttributeHelper<Object, C>(
                                (ServiceReferenceMetadata) attributeMetadata,
                                this, referenceEventHandler);

                    } else {
                        helper = new BundleCapabilityReferenceAttributeHelper<C>(
                                (BundleCapabilityReferenceMetadata) attributeMetadata, this, referenceEventHandler);

                    }
                } catch (IllegalAccessException e) {
                    fail(e, true);
                    return;
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
    public ServiceReference<?> getComponentServiceReference() {
        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            if (serviceRegistration == null) {
                return null;
            }
            return serviceRegistration.getReference();
        } finally {
            readLock.unlock();
        }
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
        return properties;
    }

    private boolean isFailed() {
        return ComponentState.FAILED == state || ComponentState.FAILED_PERMANENT == state;
    }

    @Override
    public boolean isSatisfied() {
        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        boolean result = satisfiedCapabilities == referenceHelpers.size();
        readLock.unlock();
        return result;
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
                for (Iterator<ReferenceHelper<?, C, ?>> iterator = referenceHelpers.iterator(); iterator.hasNext()
                        && state == ComponentState.UNSATISFIED;) {
                    ReferenceHelper<?, C, ?> referenceAttributeHelper = iterator.next();
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

    private Map<String, Object> resolveProperties(Dictionary<String, Object> props) {
        Map<String, Object> result = new HashMap<String, Object>();

        if (props != null) {
            Enumeration<Object> elements = props.elements();
            Enumeration<String> keys = props.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                Object element = elements.nextElement();
                result.put(key, element);
            }
        }

        AttributeMetadata<?>[] attributes = componentMetadata.getAttributes();
        for (AttributeMetadata<?> attributeMetadata : attributes) {
            String attributeId = attributeMetadata.getAttributeId();
            Object attributeValue = result.get(attributeId);
            if (attributeValue == null) {
                Object[] defaultValue = attributeMetadata.getDefaultValue();
                if (attributeMetadata.isMultiple() && defaultValue != null) {
                    result.put(attributeId, defaultValue);
                } else if (defaultValue != null && defaultValue.length > 0) {
                    result.put(attributeId, defaultValue[0]);
                }
            }
        }

        return result;
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
            if (state != ComponentState.ACTIVE) {
                throw new IllegalStateException(
                        "Only ACTIVE components can be restarted, while the state of the component "
                                + componentMetadata.getComponentId() + " is " + state.toString());
            }
            stopping(ComponentState.STOPPING);
            starting();
        } finally {
            writeLock.unlock();
        }
    }

    private boolean shouldRestartForNewConfiguraiton(Map<String, Object> newProperties) {
        AttributeMetadata<?>[] componentAttributes = componentMetadata.getAttributes();
        for (AttributeMetadata<?> attributeMetadata : componentAttributes) {
            if (!attributeMetadata.isDynamic()) {
                String attributeId = attributeMetadata.getAttributeId();
                Object oldValue = properties.get(attributeId);
                Object newValue = newProperties.get(attributeId);
                if (!equals(oldValue, newValue)) {
                    return false;
                }
            }
        }
        return false;
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

            for (ReferenceHelper<?, C, ?> referenceHelper : referenceHelpers) {
                referenceHelper.bind();
            }

            if (isFailed()) {
                return;
            }

            try {
                for (PropertyAttributeHelper<C, Object> helper : propertyAttributeHelpers) {
                    PropertyAttributeMetadata<Object> attributeMetadata = helper.getAttributeMetadata();
                    String attributeId = attributeMetadata.getAttributeId();
                    Object propertyValue = properties.get(attributeId);
                    helper.validate(propertyValue);
                    helper.applyValue(propertyValue);
                    if (isFailed()) {
                        return;
                    }
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
                serviceRegistration = registerService(serviceInterfaces, instance, new Hashtable<String, Object>(
                        properties));
            }
            state = ComponentState.ACTIVE;
        } finally {
            processingThread = null;
            writeLock.unlock();
        }
    }

    private void stopping(ComponentState targetState) {
        Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            state = ComponentState.STOPPING;
            processingThread = Thread.currentThread();
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
            if (instance != null) {
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
            }

            state = targetState;
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
            if (state == ComponentState.FAILED_PERMANENT) {
                System.out.println("Configuration update has no effect due to permanent failure");
                return;
            }
            Map<String, Object> newProperties = resolveProperties(properties);
            if (state == ComponentState.UNSATISFIED) {
                state = ComponentState.STOPPED;
            } else if (state == ComponentState.ACTIVE && shouldRestartForNewConfiguraiton(newProperties)) {
                stopping(ComponentState.STOPPED);
            }

            Map<String, Object> oldProperties = this.properties;
            this.properties = newProperties;
            for (ReferenceHelper<?, C, ?> referenceHelper : referenceHelpers) {
                String attributeId = referenceHelper.getReferenceMetadata().getAttributeId();

                Object newValue = newProperties.get(attributeId);
                Object oldValue = oldProperties.get(attributeId);

                if (!equals(oldValue, newValue)) {
                    referenceHelper.updateConfiguration();
                    if (isFailed()) {
                        return;
                    }
                }
            }

            if (state == ComponentState.ACTIVE) {
                try {
                    for (PropertyAttributeHelper<C, Object> helper : propertyAttributeHelpers) {
                        String attributeId = helper.getAttributeMetadata().getAttributeId();

                        Object oldValue = oldProperties.get(attributeId);
                        Object newValue = newProperties.get(attributeId);

                        if (!equals(oldValue, newValue)) {
                            helper.validate(newValue);
                            helper.applyValue(newValue);
                            if (isFailed()) {
                                return;
                            }
                        }
                    }
                } catch (ConfigurationException | RuntimeException e) {
                    fail(e, false);
                    return;
                }
            } else if (isSatisfied()) {
                starting();
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void validateComponentStateForServiceRegistration() {
        if (state != ComponentState.ACTIVE && state != ComponentState.STARTING) {
            throw new IllegalStateException(
                    "Service can only be registered in component if the state of the component is ACTIVE or STARTING");
        }
    }
}
