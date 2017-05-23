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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Generated;

import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.ECMComponentConstants;
import org.everit.osgi.ecm.component.PasswordHolder;
import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.component.ri.internal.attribute.BundleCapabilityReferenceAttributeHelper;
import org.everit.osgi.ecm.component.ri.internal.attribute.PropertyAttributeHelper;
import org.everit.osgi.ecm.component.ri.internal.attribute.PropertyAttributeUtil;
import org.everit.osgi.ecm.component.ri.internal.attribute.ReferenceHelper;
import org.everit.osgi.ecm.component.ri.internal.attribute.ServiceReferenceAttributeHelper;
import org.everit.osgi.ecm.component.ri.internal.resource.ComponentRevisionImpl;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.BundleCapabilityReferenceMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.PasswordAttributeMetadata;
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
import org.osgi.service.log.LogService;

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
    public void failedDuringConfigurationUpdate(
        final ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper) {
      Lock writeLock = readWriteLock.writeLock();
      writeLock.lock();
      try {
        satisfiedReferenceHelpers.remove(referenceHelper);
        revisionBuilder.updateSuitingsForAttribute(referenceHelper.getReferenceMetadata(),
            referenceHelper.getSuitings());
      } finally {
        writeLock.unlock();
      }
    }

    @Override
    public synchronized void satisfied(
        final ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper) {
      Lock writeLock = readWriteLock.writeLock();
      writeLock.lock();
      try {
        satisfiedReferenceHelpers.add(referenceHelper);

        // TODO do it together with state change atomically.
        revisionBuilder.updateSuitingsForAttribute(referenceHelper.getReferenceMetadata(),
            referenceHelper.getSuitings());

        if (configurationUpdateInProgress) {
          return;
        }

        ComponentState state = getState();
        if (satisfiedReferenceHelpers.size() == referenceHelpers.size()
            && (state == ComponentState.UNSATISFIED || state == ComponentState.FAILED)) {
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
        satisfiedReferenceHelpers.remove(referenceHelper);

        // TODO do it together with state change atomically.
        revisionBuilder.updateSuitingsForAttribute(referenceHelper.getReferenceMetadata(),
            referenceHelper.getSuitings());

        if (configurationUpdateInProgress) {
          // Stopping will be called in the end of configuration
          return;
        }

        ComponentState state = getState();
        switch (state) {
          case ACTIVE:
            stopping(ComponentState.UNSATISFIED);
            break;
          case FAILED:
            revisionBuilder.unsatisfied();
            break;
          case STOPPING:
            // happens in case of circular dynamic references
            referenceHelper.free();
            break;
          default:
            // Do nothing
        }
      } finally {
        writeLock.unlock();
      }
    }

    @Override
    public void updateDynamicWithoutSatisfactionChange(
        final ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper) {
      Lock writeLock = readWriteLock.writeLock();
      writeLock.lock();
      try {
        revisionBuilder.updateSuitingsForAttribute(referenceHelper.getReferenceMetadata(),
            referenceHelper.getSuitings());
        ComponentState state = getState();
        if (state == ComponentState.ACTIVE) {
          referenceHelper.bind();
          if (getState() == ComponentState.ACTIVE && !configurationUpdateInProgress) {
            callUpdateMethod();
          }
        } else if (state == ComponentState.FAILED && !configurationUpdateInProgress) {
          starting();
        }
      } finally {
        writeLock.unlock();
      }
    }

    @Override
    public void updateNonDynamic(
        final ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper) {
      Lock writeLock = readWriteLock.writeLock();
      writeLock.lock();
      try {
        ComponentState state = getState();
        if (state == ComponentState.ACTIVE) {
          revisionBuilder.stopping();
        }
        revisionBuilder.updateSuitingsForAttribute(referenceHelper.getReferenceMetadata(),
            referenceHelper.getSuitings());

        if (state == ComponentState.ACTIVE) {
          restart();
        } else if (state == ComponentState.FAILED && !configurationUpdateInProgress) {
          starting();
        }
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

  /**
   * We need to store component type name to be able to write out meaningful log messages if wicked
   * proxy technologies weave classes in the system and {@link #componentType}.getName() returns a
   * meaningless name.
   */
  private String componentTypeName;

  private boolean configurationUpdateInProgress = false;

  private Method deactivateMethod;

  private C instance;

  private final LogService logService;

  private boolean opened = false;

  private final List<PropertyAttributeHelper<C, Object>> propertyAttributeHelpers =
      new ArrayList<>();

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private final ReferenceEventHandler referenceEventHandler = new ReferenceEventHandlerImpl();

  private final List<ReferenceHelper<?, C, ?>> referenceHelpers = new ArrayList<>();

  private final ComponentRevisionImpl.Builder<C> revisionBuilder;

  private final Set<ReferenceHelper<?, ?, ?>> satisfiedReferenceHelpers = new HashSet<>();

  private String[] serviceInterfaces;

  private ServiceRegistration<?> serviceRegistration = null;

  private Method updateMethod;

  /**
   * Constructor.
   *
   * @param componentContainer
   *          The component container that created this context.
   * @param bundleContext
   *          The context of the bundle that opened the Component Container.
   * @param properties
   *          The configuration of the component or <code>null</code> if configuration does not
   *          exist.
   * @param logService
   *          The logger to forward information about events to.
   */
  public ComponentContextImpl(final AbstractComponentContainer<C> componentContainer,
      final BundleContext bundleContext, final Dictionary<String, Object> properties,
      final LogService logService) {

    this.bundleContext = bundleContext;
    this.componentContainer = componentContainer;
    this.logService = logService;

    Map<String, Object> propertyMap = createPropMapFromConfigDictionary(properties);
    this.revisionBuilder =
        new ComponentRevisionImpl.Builder<>(componentContainer, propertyMap);

    this.revisionBuilder.updateProperties(resolveProperties(propertyMap, false));

    ComponentMetadata componentMetadata = componentContainer.getComponentMetadata();
    Bundle bundle = bundleContext.getBundle();
    BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
    ClassLoader classLoader = bundleWiring.getClassLoader();
    componentTypeName = componentMetadata.getType();
    try {
      @SuppressWarnings("unchecked")
      Class<C> tmpComponentType = (Class<C>) classLoader.loadClass(componentTypeName);
      componentType = tmpComponentType;
    } catch (ClassNotFoundException e) {
      fail(e, true);
      return;
    }

    activateMethodHelper = new ActivateMethodHelper<>(this);

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

  private void addCommonComponentProperties(final Map<String, Object> properties) {
    ComponentMetadata componentMetadata = componentContainer.getComponentMetadata();

    properties.put(ECMComponentConstants.SERVICE_PROP_COMPONENT_ID,
        componentMetadata.getComponentId());
    properties.put(ECMComponentConstants.SERVICE_PROP_COMPONENT_VERSION,
        componentContainer.getVersion());
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
        stopping(ComponentState.INACTIVE);
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

  private void convertAttributeValueIfNecessary(
      final AttributeMetadata<?> attributeMetadata, final Map<String, Object> result) {

    if (attributeMetadata.isMultiple()) {
      // TODO convert multiple values or at least do a check.
      return;
    }

    String attributeId = attributeMetadata.getAttributeId();
    Object attributeValue = result.get(attributeId);

    attributeValue = PropertyAttributeUtil
        .resolveSimpleValueEvenIfItIsInOneElementArray(attributeValue, this, attributeMetadata);

    if (attributeValue == null) {
      return;
    }

    Class<? extends Object> configuredAttributeValueType = attributeValue.getClass();

    Class<?> attributeType = attributeMetadata.getValueType();
    if (PropertyAttributeUtil.typesEqualWithOrWithoutBoxing(configuredAttributeValueType,
        attributeType)) {
      return;
    }

    if (attributeValue instanceof PasswordHolder) {
      attributeType = PasswordHolder.class;
    }

    Object newAttributeValue =
        PropertyAttributeUtil.tryConvertingSimpleValue(attributeValue, attributeType,
            this, attributeMetadata);

    result.put(attributeId, newAttributeValue);
  }

  private Map<String, Object> createPropMapFromConfigDictionary(final Dictionary<String, ?> props) {
    Map<String, Object> result = new HashMap<>();

    if (props != null) {
      Enumeration<?> elements = props.elements();
      Enumeration<String> keys = props.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        Object element = elements.nextElement();
        result.put(key, element);
      }
    }
    return result;
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
    boolean stopComponent = false;
    if (getState() == ComponentState.ACTIVE) {
      stopComponent = true;
    }
    fail(e, permanent, stopComponent);
  }

  private void fail(final Throwable e, final boolean permanent, final boolean stopComponent) {
    logService.log(LogService.LOG_ERROR,
        "Component error: {id: '" + componentContainer.getComponentMetadata().getComponentId()
            + "', state: " + revisionBuilder.getState().toString() + ", properties: "
            + revisionBuilder.getProperties().toString() + "}",
        e);
    revisionBuilder.setOrAddSuppressedCause(e);
    if (isFailed()) {
      return;
    }

    if (stopComponent) {
      stopping(ComponentState.FAILED);
    } else {
      instance = null;
    }

    revisionBuilder.fail(e, permanent);
  }

  private void fillAttributeHelpers(final AttributeMetadata<?>[] attributes) {
    for (AttributeMetadata<?> attributeMetadata : attributes) {
      if (attributeMetadata instanceof PropertyAttributeMetadata) {

        @SuppressWarnings("unchecked")
        PropertyAttributeHelper<C, Object> propertyAttributeHelper =
            new PropertyAttributeHelper<>(this,
                (PropertyAttributeMetadata<Object>) attributeMetadata);

        propertyAttributeHelpers
            .add(propertyAttributeHelper);
      } else {
        ReferenceHelper<?, C, ?> helper;
        try {
          if (attributeMetadata instanceof ServiceReferenceMetadata) {

            helper = new ServiceReferenceAttributeHelper<>(
                (ServiceReferenceMetadata) attributeMetadata,
                this, referenceEventHandler);

          } else {
            helper = new BundleCapabilityReferenceAttributeHelper<>(
                (BundleCapabilityReferenceMetadata) attributeMetadata, this, referenceEventHandler);

          }
        } catch (IllegalAccessException | RuntimeException e) {
          fail(e, true);
          return;
        }
        referenceHelpers.add(helper);
      }
    }
  }

  private void freeReferences() {
    for (ReferenceHelper<?, C, ?> referenceHelper : referenceHelpers) {
      referenceHelper.free();
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

  public String getComponentTypeName() {
    return componentTypeName;
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
    return satisfiedReferenceHelpers.size() == referenceHelpers.size();
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
      if (getState() != ComponentState.INACTIVE) {
        return;
      }

      try {
        revisionBuilder.updateProperties(resolveProperties(revisionBuilder.getProperties(), true));
      } catch (RuntimeException e) {
        revisionBuilder.updateProperties(resolveProperties(revisionBuilder.getProperties(), false));
        fail(e, false);
        return;
      }

      if (referenceHelpers.size() == 0) {
        starting();
      } else {
        for (ReferenceHelper<?, C, ?> referenceAttributeHelper : referenceHelpers) {
          referenceAttributeHelper.open();
        }
        if (isFailed()) {
          return;
        }
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
        new ComponentServiceRegistration<>(this, original);
    revisionBuilder.addServiceRegistration(componentServiceRegistration);
    return componentServiceRegistration;
  }

  void removeServiceRegistration(final ServiceRegistration<?> pServiceRegistration) {
    revisionBuilder.removeServiceRegistration(pServiceRegistration);
  }

  private void replacePasswordStringsToPasswordHoldersInProperties(final Map<String, Object> result,
      final AttributeMetadata<?> attributeMetadata) {
    String attributeId = attributeMetadata.getAttributeId();
    if (attributeMetadata instanceof PasswordAttributeMetadata
        && result.containsKey(attributeId)) {

      Object password = result.get(attributeId);
      if (password != null) {
        if (password instanceof String) {
          result.put(attributeId, new PasswordHolder((String) password));
        } else if (password instanceof String[]) {
          String[] passwordStringArray = (String[]) password;
          PasswordHolder[] passwordHolderArray = new PasswordHolder[passwordStringArray.length];
          for (int i = 0; i < passwordStringArray.length; i++) {
            passwordHolderArray[i] = new PasswordHolder(passwordStringArray[i]);
          }
          result.put(attributeId, passwordHolderArray);
        }
      }
    }
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
          + "' for type " + componentTypeName);
      fail(exception, true);
    }
    if (method.getParameterTypes().length > 0) {
      Exception exception = new IllegalMetadataException(
          methodType.substring(0, 1).toUpperCase(Locale.getDefault()) + methodType.substring(1)
              + " method must not have any parameters. Method '"
              + method.toGenericString() + "' of type " + componentTypeName + " does have.");
      fail(exception, true);
    }
    return method;
  }

  private Map<String, Object> resolveProperties(final Map<String, Object> props,
      final boolean doConversionIfNecessary) {

    Map<String, Object> result = new HashMap<>(props);
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
      } else if (doConversionIfNecessary) {
        convertAttributeValueIfNecessary(attributeMetadata, result);
      }

      replacePasswordStringsToPasswordHoldersInProperties(result, attributeMetadata);
    }

    addCommonComponentProperties(result);

    return Collections.unmodifiableMap(result);
  }

  private String[] resolveServiceInterfaces() {
    ServiceMetadata serviceMetadata = componentContainer.getComponentMetadata().getService();
    if (serviceMetadata == null) {
      return new String[0];
    }

    String[] clazzes = serviceMetadata.getClazzes();
    if (clazzes.length > 0) {
      return clazzes;
    }

    // Auto detect
    Set<String> interfaces = new LinkedHashSet<>();
    Class<?> currentClass = componentType;
    ComponentContextImpl.resolveSuperInterfacesRecurse(currentClass, interfaces);
    interfaces.add(componentTypeName);

    return interfaces.toArray(new String[interfaces.size()]);
  }

  private void restart() {
    stopping(ComponentState.STOPPING);
    if (isSatisfied()) {
      starting();
    } else {
      // Happens when component was active and it could wire itself (circular wiring) but service
      // disappears due to stopping the component.
      revisionBuilder.unsatisfied();
    }

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
      } catch (IllegalAccessException e) {
        fail(e, true);
        return;
      } catch (InvocationTargetException e) {
        fail(e.getCause(), false, true);
        return;
      }

      if (serviceInterfaces.length > 0) {
        serviceRegistration = registerService(serviceInterfaces, instance,
            new Hashtable<>(properties));
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
            logService.log(LogService.LOG_ERROR, "Component error: {id: '"
                + componentContainer.getComponentMetadata().getComponentId()
                + "', state: " + revisionBuilder.getState().toString() + ", properties: "
                + revisionBuilder.getProperties().toString() + "}", e);
            if (targetState == ComponentState.FAILED) {
              revisionBuilder.setOrAddSuppressedCause(e);
            } else {
              e.printStackTrace(System.err);
            }
          }
        }
        unregisterServices();
        freeReferences();
        instance = null;
      }

    } finally {
      switch (targetState) {
        case INACTIVE:
          revisionBuilder.inactive();
          break;
        case UNSATISFIED:
          revisionBuilder.unsatisfied();
          break;
        case UPDATING_CONFIGURATION:
          revisionBuilder.updatingConfiguration();
          break;
        default:
          // Do nothing as in case STOPPING state remains the same, in case of FAILED* the state
          // will be set by the caller function
          break;
      }
    }
  }

  private void unregisterServices() {
    for (ServiceRegistration<?> lServiceRegistration : revisionBuilder
        .getCloneOfServiceRegistrations()) {
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
    configurationUpdateInProgress = true;
    try {
      if (getState() == ComponentState.FAILED_PERMANENT) {
        return;
      }
      updateConfigurationInLock(properties);
    } finally {
      configurationUpdateInProgress = false;
      writeLock.unlock();
    }
  }

  private void updateConfigurationInLock(final Dictionary<String, ?> properties) {

    ComponentState stateBeforeUpdate = getState();

    Map<String, Object> oldProperties = getProperties();
    Map<String, Object> newPropertyMap = createPropMapFromConfigDictionary(properties);

    Map<String, Object> newProperties;
    try {
      newProperties = resolveProperties(newPropertyMap, true);
    } catch (RuntimeException e) {
      revisionBuilder.updateProperties(resolveProperties(newPropertyMap, false));
      fail(e, false);
      return;
    }

    // After this preparing, the component will be either UPDATING_CONFIGURATION or ACTIVE
    prepareConfigurationUpdate(stateBeforeUpdate, newProperties);

    revisionBuilder.updateProperties(newProperties);

    updateReferences(newProperties, oldProperties);

    ComponentState stateAfterReferenceUpdate = getState();
    if (stateAfterReferenceUpdate == ComponentState.UNSATISFIED || isFailed()) {
      return;
    }

    if (!isSatisfied()) {
      if (getState() == ComponentState.ACTIVE) {
        stopping(ComponentState.UNSATISFIED);
      } else {
        revisionBuilder.unsatisfied();
      }
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

    if (serviceRegistration != null) {
      serviceRegistration.setProperties(new Hashtable<>(newProperties));
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
