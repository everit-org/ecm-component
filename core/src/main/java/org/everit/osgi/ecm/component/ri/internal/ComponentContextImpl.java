/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.ecm.component.ri.internal;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Generated;

import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.component.ri.internal.attribute.BundleCapabilityReferenceAttributeHelper;
import org.everit.osgi.ecm.component.ri.internal.attribute.PropertyAttributeHelper;
import org.everit.osgi.ecm.component.ri.internal.attribute.ReferenceHelper;
import org.everit.osgi.ecm.component.ri.internal.attribute.ServiceReferenceAttributeHelper;
import org.everit.osgi.ecm.component.ri.internal.resource.ComponentRevisionImpl;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.BundleCapabilityReferenceMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.MetadataValidationException;
import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;
import org.everit.osgi.ecm.metadata.ServiceMetadata;
import org.everit.osgi.ecm.metadata.ServiceReferenceMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

/**
 * The context of a component instance. This is the most important class that manages the entire
 * lifecycle of one instance of the component.
 *
 * @param <C>
 *          The type of the component implementation.
 */
public class ComponentContextImpl<C> implements ComponentContext<C> {

  /**
   * Event handler that catches all events of references and based on satisfaction, starts or
   * unsatisfies the component instance.
   */
  private class ReferenceEventHandlerImpl implements ReferenceEventHandler {

    @Override
    public synchronized void satisfied(
        final ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper) {
      Lock writeLock = readWriteLock.writeLock();
      writeLock.lock();
      try {
        satisfiedReferences++;

        // TODO do it together with state change atomically.
        revisionBuilder.updateSuitingsForAttribute(referenceHelper.getReferenceMetadata(),
            referenceHelper.getSuitings());

        ComponentState state = getState();
        if (satisfiedReferences == referenceHelpers.size() && state == ComponentState.UNSATISFIED) {
          starting();
        }
      } finally {
        writeLock.unlock();
      }
    }

    @Override
    public synchronized void unsatisfied(
        final ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper) {
      Lock writeLock = readWriteLock.writeLock();
      writeLock.lock();
      try {
        satisfiedReferences--;

        // TODO do it together with state change atomically.
        revisionBuilder.updateSuitingsForAttribute(referenceHelper.getReferenceMetadata(),
            referenceHelper.getSuitings());

        ComponentState state = getState();
        if (state == ComponentState.ACTIVE) {
          stopping(ComponentState.UNSATISFIED);
        }
      } finally {
        writeLock.unlock();
      }
    }

    @Override
    public void updateDynamicWithoutSatisfactionChange(
        final ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper) {
      revisionBuilder.updateSuitingsForAttribute(referenceHelper.getReferenceMetadata(),
          referenceHelper.getSuitings());
      if (getState() == ComponentState.ACTIVE) {
        referenceHelper.bind();
      }
    }

    @Override
    public void updateNonDynamic(
        final ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper) {
      Lock writeLock = readWriteLock.writeLock();
      writeLock.lock();
      try {
        if (getState() == ComponentState.ACTIVE) {
          restart();
        }
        // TODO do it together with state change atomically.
        revisionBuilder.updateSuitingsForAttribute(referenceHelper.getReferenceMetadata(),
            referenceHelper.getSuitings());
      } finally {
        writeLock.unlock();
      }
    }

  }

  private static void resolveSuperInterfacesRecurse(final Class<?> currentClass,
      final Set<String> interfaces) {
    Class<?>[] superInterfaces = currentClass.getInterfaces();
    for (Class<?> superInterface : superInterfaces) {
      interfaces.add(superInterface.getName());
      ComponentContextImpl.resolveSuperInterfacesRecurse(superInterface, interfaces);
    }
  }

  private ActivateMethodHelper<C> activateMethodHelper;

  private final BundleContext bundleContext;

  private final AbstractComponentContainer<C> componentContainer;

  private Class<C> componentType;

  private Method deactivateMethod;

  private C instance;

  private boolean opened = false;

  private final List<PropertyAttributeHelper<C, Object>> propertyAttributeHelpers =
      new ArrayList<>();

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private final ReferenceEventHandler referenceEventHandler = new ReferenceEventHandlerImpl();

  private final List<ReferenceHelper<?, C, ?>> referenceHelpers = new ArrayList<>();

  private final ComponentRevisionImpl.Builder<C> revisionBuilder;

  private int satisfiedReferences = 0;

  private String[] serviceInterfaces;

  private ServiceRegistration<?> serviceRegistration = null;

  private Method updateMethod;

  public ComponentContextImpl(final AbstractComponentContainer<C> componentContainer,
      final BundleContext bundleContext) {
    this(componentContainer, bundleContext, null);
  }

  /**
   * Constructor.
   *
   * @param componentContainer
   *          The component container that created this context.
   * @param bundleContext
   *          The context of the bundle that opened the Component Container.
   * @param properties
   *          The configuration of the component.
   */
  public ComponentContextImpl(final AbstractComponentContainer<C> componentContainer,
      final BundleContext bundleContext, final Dictionary<String, Object> properties) {

    this.bundleContext = bundleContext;
    this.componentContainer = componentContainer;
    this.revisionBuilder = new ComponentRevisionImpl.Builder<C>(componentContainer,
        resolveProperties(properties));

    if (isFailed()) {
      return;
    }

    ComponentMetadata componentMetadata = componentContainer.getComponentMetadata();
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

    activateMethodHelper = new ActivateMethodHelper<C>(this);

    if (isFailed()) {
      return;
    }

    AttributeMetadata<?>[] attributes = componentMetadata.getAttributes();

    fillAttributeHelpers(attributes);

    serviceInterfaces = resolveServiceInterfaces();

    deactivateMethod = resolveAnnotatedMethod("deactivate", componentContainer
        .getComponentMetadata().getDeactivate());

    updateMethod = resolveAnnotatedMethod("update", componentContainer
        .getComponentMetadata().getUpdate());

  }

  private void callUpdateMethod() {
    if (updateMethod != null) {
      try {
        updateMethod.invoke(instance);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        fail(e, false);
      }
    }
  }

  /**
   * Closing the component context that stops the component instance as well if it is started.
   */
  public void close() {
    Lock writeLock = readWriteLock.writeLock();
    writeLock.lock();
    try {
      if (!opened) {
        throw new IllegalStateException("Cannot close a component context that is not opened");
      }
      opened = false;
      if (getState() == ComponentState.ACTIVE) {
        stopping(ComponentState.STOPPED);
      } else {
        closeReferenceHelpers();
      }
    } finally {
      writeLock.unlock();
    }
  }

  private void closeReferenceHelpers() {
    for (ReferenceHelper<?, C, ?> referenceHelper : referenceHelpers) {
      if (referenceHelper.isOpened()) {
        referenceHelper.close();
      }
    }
  }

  @Generated("eclipse")
  private boolean equals(final Object oldValue, final Object newValue) {
    if (((oldValue == null) && (newValue != null)) || ((oldValue != null) && (newValue == null))) {
      return false;
    }
    if ((oldValue != null) && !oldValue.equals(newValue)) {

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

  /**
   * Sets FAILED state for the ComponentContext, unregisters the OSGi services registered via the
   * {@link ComponentContext} and removes the instance object.
   *
   * @param e
   *          The cause of the component failure.
   * @param permanent
   *          Whether the failure is permanent or not. In case of non-permanent failure, a
   *          configuration update will change the state of the component, while a permanent failure
   *          can be changed only by upgrading the component instance binary.
   */
  public void fail(final Throwable e, final boolean permanent) {
    revisionBuilder.fail(e, permanent);

    unregisterServices();
    instance = null;

    return;
  }

  private void fillAttributeHelpers(final AttributeMetadata<?>[] attributes) {
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
        } catch (IllegalAccessException | MetadataValidationException e) {
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
  public ComponentContainer<C> getComponentContainer() {
    return componentContainer;
  }

  @Override
  public ComponentRevisionImpl<C> getComponentRevision() {
    return revisionBuilder.build();
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
    return revisionBuilder.getProperties();
  }

  public ComponentState getState() {
    return revisionBuilder.getState();
  }

  public boolean isFailed() {
    ComponentState state = getState();
    return (ComponentState.FAILED == state) || (ComponentState.FAILED_PERMANENT == state);
  }

  /**
   * Whether the component is satisfied based on the currently satisfied references or not.
   *
   * @return true if all of the references are satisfied, otherwise false.
   */
  public boolean isSatisfied() {
    return satisfiedReferences == referenceHelpers.size();
  }

  /**
   * Opens the component that means that configuration will be processed, all references will be
   * tracked and if all references are satisfied the component will be started.
   */
  public void open() {
    Lock writeLock = readWriteLock.writeLock();
    writeLock.lock();

    try {
      if (opened) {
        throw new IllegalStateException("Cannot open a component context that is already opened");
      }
      opened = true;
      if (getState() != ComponentState.STOPPED) {
        return;
      }
      revisionBuilder.updateProperties(revisionBuilder.getProperties());
      if (referenceHelpers.size() == 0) {
        starting();
      } else {
        for (ReferenceHelper<?, C, ?> referenceAttributeHelper : referenceHelpers) {
          referenceAttributeHelper.open();
        }
        // TODO multi-threading issue might come here
        if (isSatisfied()) {
          starting();
        } else {
          revisionBuilder.unsatisfied();
        }
      }
    } finally {
      writeLock.unlock();
    }
  }

  private void prepareConfigurationUpdate(final ComponentState stateBeforeUpdate,
      final Map<String, Object> newProperties) {

    if (stateBeforeUpdate == ComponentState.ACTIVE
        && shouldRestartForNewConfiguraiton(newProperties)) {
      stopping(ComponentState.UPDATING_CONFIGURATION);
    } else if (stateBeforeUpdate == ComponentState.UNSATISFIED
        || stateBeforeUpdate == ComponentState.FAILED) {
      revisionBuilder.updatingConfiguration();
    }
  }

  @Override
  public <S> ServiceRegistration<S> registerService(final Class<S> clazz, final S service,
      final Dictionary<String, ?> properties) {

    validateComponentStateForServiceRegistration();
    ServiceRegistration<S> lServiceRegistration = bundleContext.registerService(clazz, service,
        properties);
    return registerServiceInternal(lServiceRegistration);
  }

  @Override
  public ServiceRegistration<?> registerService(final String clazz, final Object service,
      final Dictionary<String, ?> properties) {
    validateComponentStateForServiceRegistration();
    ServiceRegistration<?> lServiceRegistration = bundleContext.registerService(clazz, service,
        properties);
    return registerServiceInternal(lServiceRegistration);

  }

  @Override
  public ServiceRegistration<?> registerService(final String[] clazzes, final Object service,
      final Dictionary<String, ?> properties) {
    validateComponentStateForServiceRegistration();
    ServiceRegistration<?> lServiceRegistration = bundleContext.registerService(clazzes, service,
        properties);
    return registerServiceInternal(lServiceRegistration);
  }

  private <S> ServiceRegistration<S> registerServiceInternal(
      final ServiceRegistration<S> original) {

    ComponentServiceRegistration<S, C> componentServiceRegistration =
        new ComponentServiceRegistration<S, C>(this, original);
    revisionBuilder.addServiceRegistration(componentServiceRegistration);
    return componentServiceRegistration;
  }

  void removeServiceRegistration(final ServiceRegistration<?> pServiceRegistration) {
    revisionBuilder.removeServiceRegistration(pServiceRegistration);
  }

  private Method resolveAnnotatedMethod(final String methodType,
      final MethodDescriptor methodDescriptor) {
    if (methodDescriptor == null) {
      return null;
    }
    Method method = methodDescriptor.locate(componentType, false);
    if (method == null) {
      Exception exception = new IllegalMetadataException("Could not find " + methodType
          + " method '" + methodDescriptor.toString()
          + "' for type " + componentType);
      fail(exception, true);
    }
    if (method.getParameterTypes().length > 0) {
      Exception exception = new IllegalMetadataException(
          methodType.substring(0, 1).toUpperCase(Locale.getDefault()) + methodType.substring(1)
              + " method must not have any parameters. Method '"
              + method.toGenericString() + "' of type " + componentType + " does have.");
      fail(exception, true);
    }
    return method;
  }

  private Map<String, Object> resolveProperties(final Dictionary<String, ?> props) {
    Map<String, Object> result = new HashMap<String, Object>();

    if (props != null) {
      Enumeration<?> elements = props.elements();
      Enumeration<String> keys = props.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        Object element = elements.nextElement();
        result.put(key, element);
      }
    }

    AttributeMetadata<?>[] attributes = componentContainer.getComponentMetadata().getAttributes();
    for (AttributeMetadata<?> attributeMetadata : attributes) {
      String attributeId = attributeMetadata.getAttributeId();
      if (!result.containsKey(attributeId)) {
        Object defaultValue = attributeMetadata.getDefaultValue();
        if (attributeMetadata.isMultiple() && (defaultValue != null)) {
          result.put(attributeId, defaultValue);
        } else if ((defaultValue != null) && (Array.getLength(defaultValue) > 0)) {
          Object value = Array.get(defaultValue, 0);
          if (value != null) {
            result.put(attributeId, value);
          }
        }
      }
    }

    return Collections.unmodifiableMap(result);
  }

  private String[] resolveServiceInterfaces() {
    ServiceMetadata serviceMetadata = componentContainer.getComponentMetadata().getService();
    if (serviceMetadata == null) {
      return new String[0];
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
    ComponentContextImpl.resolveSuperInterfacesRecurse(currentClass, interfaces);

    if (interfaces.size() != 0) {
      return interfaces.toArray(new String[interfaces.size()]);
    }
    return new String[] { componentType.getName() };
  }

  private void restart() {
    ComponentState state = getState();
    if (state != ComponentState.ACTIVE) {
      throw new IllegalStateException(
          "Only ACTIVE components can be restarted, while the state of the component "
              + componentContainer.getComponentMetadata().getComponentId() + " is "
              + state.toString());
    }
    stopping(ComponentState.STOPPING);
    starting();
  }

  private boolean shouldRestartForNewConfiguraiton(final Map<String, Object> newProperties) {
    AttributeMetadata<?>[] componentAttributes = componentContainer.getComponentMetadata()
        .getAttributes();
    Map<String, Object> properties = getProperties();
    for (AttributeMetadata<?> attributeMetadata : componentAttributes) {
      if (!attributeMetadata.isDynamic()) {
        String attributeId = attributeMetadata.getAttributeId();

        Object oldValue = properties.get(attributeId);
        Object newValue = newProperties.get(attributeId);
        if (!equals(oldValue, newValue)) {
          return true;
        }
      }
    }
    return false;
  }

  private void starting() {
    Lock writeLock = readWriteLock.writeLock();
    writeLock.lock();
    try {
      if (getState() == ComponentState.FAILED_PERMANENT) {
        return;
      }

      revisionBuilder.starting();

      try {
        instance = componentType.newInstance();
      } catch (InstantiationException | IllegalAccessException | RuntimeException e) {
        fail(e, true);
        return;
      }

      for (ReferenceHelper<?, C, ?> referenceHelper : referenceHelpers) {
        referenceHelper.bind();
        if (isFailed()) {
          return;
        }
      }

      Map<String, Object> properties = getProperties();
      try {
        for (PropertyAttributeHelper<C, Object> helper : propertyAttributeHelpers) {
          PropertyAttributeMetadata<Object> attributeMetadata = helper.getAttributeMetadata();
          String attributeId = attributeMetadata.getAttributeId();
          Object propertyValue = properties.get(attributeId);
          helper.applyValue(propertyValue);
          if (isFailed()) {
            return;
          }
        }
        activateMethodHelper.call(instance);
      } catch (RuntimeException | ReflectiveOperationException e) {
        fail(e, false);
        return;
      }

      if (serviceInterfaces.length > 0) {
        serviceRegistration = registerService(serviceInterfaces, instance,
            new Hashtable<String, Object>(
                properties));
      }
      revisionBuilder.active();
    } finally {
      writeLock.unlock();
    }
  }

  private void stopping(final ComponentState targetState) {
    try {
      revisionBuilder.stopping();
      if (serviceRegistration != null) {
        serviceRegistration.unregister();
        serviceRegistration = null;
      }
      if (instance != null) {
        if (deactivateMethod != null) {
          try {
            deactivateMethod.invoke(instance);
          } catch (IllegalAccessException | IllegalArgumentException
              | InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace(System.err);
          }
        }
        unregisterServices();
        instance = null;
      }

    } finally {
      switch (targetState) {
        case STOPPED:
          revisionBuilder.stopped();
          break;
        case UNSATISFIED:
          revisionBuilder.unsatisfied();
          break;
        case UPDATING_CONFIGURATION:
          revisionBuilder.updatingConfiguration();
          break;
        case STOPPING:
          break;
        default:
          throw new RuntimeException("Target state " + targetState
              + " is not allowed after stopping");
      }
    }
  }

  private void unregisterServices() {
    if (serviceRegistration != null) {
      serviceRegistration.unregister();
    }
    for (ServiceRegistration<?> lServiceRegistration : revisionBuilder
        .getCloneOfServiceRegistrations()) {
      revisionBuilder.removeServiceRegistration(lServiceRegistration);
      lServiceRegistration.unregister();
    }
  }

  /**
   * Updates the configuration on the component instance. In case non-dynamic properties or
   * references are changed, the component instance will be restarted (during restart the old object
   * will be dropped).
   *
   * @param properties
   *          The new configuration of the component.
   */
  public void updateConfiguration(final Dictionary<String, ?> properties) {
    Lock writeLock = readWriteLock.writeLock();
    writeLock.lock();
    try {
      if (getState() == ComponentState.FAILED_PERMANENT) {
        return;
      }
      updateConfigurationInLock(properties);
    } finally {
      writeLock.unlock();
    }
  }

  private void updateConfigurationInLock(final Dictionary<String, ?> properties) {
    ComponentState stateBeforeUpdate = getState();

    Map<String, Object> oldProperties = getProperties();
    Map<String, Object> newProperties = resolveProperties(properties);

    // After this preparing, the component will be either UPDATING_CONFIGURATION or ACTIVE
    prepareConfigurationUpdate(stateBeforeUpdate, newProperties);

    revisionBuilder.updateProperties(newProperties);

    updateReferences(newProperties, oldProperties);

    ComponentState stateAfterReferenceUpdate = getState();
    if (stateAfterReferenceUpdate == ComponentState.UNSATISFIED || isFailed()) {
      return;
    }

    if (!isSatisfied()) {
      revisionBuilder.unsatisfied();
    } else if (stateAfterReferenceUpdate == ComponentState.UPDATING_CONFIGURATION) {
      starting();
    } else {
      // This means that the component is active after references are updated
      updatePropertiesOnComponentInstance(newProperties, oldProperties);

      if (isFailed()) {
        return;
      }

      callUpdateMethod();
    }
  }

  private void updatePropertiesOnComponentInstance(final Map<String, Object> newProperties,
      final Map<String, Object> oldProperties) {
    for (PropertyAttributeHelper<C, Object> helper : propertyAttributeHelpers) {
      String attributeId = helper.getAttributeMetadata().getAttributeId();

      Object oldValue = oldProperties.get(attributeId);
      Object newValue = newProperties.get(attributeId);

      if (!equals(oldValue, newValue)) {
        helper.applyValue(newValue);

        if (isFailed()) {
          return;
        }
      }
    }
  }

  private void updateReferences(final Map<String, Object> newProperties,
      final Map<String, Object> oldProperties) {
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
      if (!referenceHelper.isOpened()) {
        referenceHelper.open();
      }
    }
  }

  private void validateComponentStateForServiceRegistration() {
    ComponentState state = getState();
    if ((state != ComponentState.ACTIVE) && (state != ComponentState.STARTING)) {
      throw new IllegalStateException(
          "Service can only be registered in component if the state of the "
              + "component is ACTIVE or STARTING");
    }
  }
}
