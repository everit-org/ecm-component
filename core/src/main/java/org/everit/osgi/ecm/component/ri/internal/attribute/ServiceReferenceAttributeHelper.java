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
package org.everit.osgi.ecm.component.ri.internal.attribute;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.everit.osgi.capabilitycollector.AbstractCapabilityCollector;
import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.everit.osgi.capabilitycollector.ServiceReferenceCollector;
import org.everit.osgi.capabilitycollector.Suiting;
import org.everit.osgi.ecm.component.ServiceHolder;
import org.everit.osgi.ecm.component.ri.internal.ComponentContextImpl;
import org.everit.osgi.ecm.component.ri.internal.ReferenceEventHandler;
import org.everit.osgi.ecm.metadata.ServiceReferenceMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;

/**
 * {@link ReferenceHelper} for OSGi service references.
 *
 * @param <S>
 *          Type of the OSGi service.
 * @param <COMPONENT>
 *          Type of the component implementation.
 */
public class ServiceReferenceAttributeHelper<S, COMPONENT> extends
    ReferenceHelper<ServiceReference<S>, COMPONENT, ServiceReferenceMetadata> {

  /**
   * {@link ServiceObjects} with counter, where counter tells how many times the
   * {@link ServiceObjects} was used to get service.
   *
   * @param <S>
   *          The type of the OSGi service.
   */
  private static class ServiceObjectsWithCounter<S> {
    public int counter = 0;

    public ServiceObjects<S> serviceObjects;
  }

  /**
   * Mapping of {@link Suiting}s and the requested OSGi service instance (suiting contain only
   * service reference).
   *
   * @param <S>
   *          The type of the OSGi service.
   */
  private static class SuitingWithService<S> {
    public S service;

    public Suiting<ServiceReference<S>> suiting;
  }

  private Map<String, SuitingWithService<S>> previousSuitingsByRequirementId = new TreeMap<>();

  private Class<S> serviceClass;

  private final Map<ServiceReference<S>, ServiceObjectsWithCounter<S>> serviceObjectsByReferences =
      new HashMap<>();

  public ServiceReferenceAttributeHelper(final ServiceReferenceMetadata referenceMetadata,
      final ComponentContextImpl<COMPONENT> componentContext,
      final ReferenceEventHandler eventHandler)
      throws IllegalAccessException {

    super(referenceMetadata, componentContext, eventHandler);
  }

  private S addToUsedServiceReferences(final ServiceReference<S> serviceReference) {
    ServiceObjectsWithCounter<S> serviceObjectsWithCounter = serviceObjectsByReferences
        .get(serviceReference);
    if (serviceObjectsWithCounter == null) {
      serviceObjectsWithCounter = new ServiceObjectsWithCounter<>();
      serviceObjectsWithCounter.serviceObjects = getComponentContext().getBundleContext()
          .getServiceObjects(serviceReference);
      serviceObjectsByReferences.put(serviceReference, serviceObjectsWithCounter);
    }

    S service = serviceObjectsWithCounter.serviceObjects.getService();

    serviceObjectsWithCounter.counter++;
    return service;
  }

  @Override
  protected synchronized void bindInternal() {

    Map<String, SuitingWithService<S>> newSuitingMapping = new TreeMap<>();
    Suiting<ServiceReference<S>>[] tmpSuitings = getSuitings();

    Object[] parameter = resolveParameterArray(tmpSuitings);

    for (int i = 0; i < tmpSuitings.length; i++) {
      Suiting<ServiceReference<S>> suiting = tmpSuitings[i];
      ServiceReference<S> serviceReference = suiting.getCapability();
      RequirementDefinition<ServiceReference<S>> requirement = suiting.getRequirement();

      String requirementId = suiting.getRequirement().getRequirementId();

      SuitingWithService<S> previousSuitingWithService = previousSuitingsByRequirementId
          .get(requirementId);

      S service;
      if ((previousSuitingWithService == null)
          || (previousSuitingWithService.suiting.getCapability()
              .compareTo(suiting.getCapability()) != 0)) {

        service = addToUsedServiceReferences(serviceReference);
        if (service != null) {
          SuitingWithService<S> suitingWithService = new SuitingWithService<S>();
          suitingWithService.service = service;
          suitingWithService.suiting = suiting;
          newSuitingMapping.put(requirementId, suitingWithService);
        }
      } else {
        previousSuitingsByRequirementId.remove(requirementId);
        service = previousSuitingWithService.service;
        newSuitingMapping.put(requirementId, previousSuitingWithService);
      }

      if (isHolder()) {
        ServiceHolder<S> serviceHolder = new ServiceHolder<S>(suiting.getRequirement()
            .getRequirementId(), serviceReference, service, requirement.getAttributes());

        parameter[i] = serviceHolder;
      } else {
        parameter[i] = service;
      }

    }

    callSetterWithParameters(parameter);

    Collection<SuitingWithService<S>> previousSuitings = previousSuitingsByRequirementId.values();
    for (SuitingWithService<S> suitingWithService : previousSuitings) {
      removeFromUsedServiceReferences(suitingWithService);
    }

    previousSuitingsByRequirementId = newSuitingMapping;
  }

  private void callSetterWithParameters(final Object[] parameter) {
    MethodHandle setterMethod = getSetterMethodHandle();
    if (isArray()) {
      try {
        setterMethod.invoke(getComponentContext().getInstance(), (Object) parameter);
      } catch (Throwable e) {
        getComponentContext().fail(e, false);
      }
    } else {
      try {
        if (parameter.length == 0) {
          setterMethod.invoke(getComponentContext().getInstance(), null);
        } else {
          setterMethod.invoke(getComponentContext().getInstance(), parameter[0]);
        }
      } catch (Throwable e) {
        getComponentContext().fail(e, false);
      }

    }
  }

  @Override
  public synchronized void close() {
    super.close();

    releaseServices();
  }

  @Override
  protected AbstractCapabilityCollector<ServiceReference<S>> createCollector(
      final ReferenceCapabilityConsumer consumer,
      final RequirementDefinition<ServiceReference<S>>[] items) {

    return new ServiceReferenceCollector<S>(getComponentContext().getBundleContext(),
        serviceClass, items, consumer, false);
  }

  @Override
  public void free() {
    releaseServices();
  }

  @Override
  protected void init() {
    String serviceInterfaceName = getReferenceMetadata().getServiceInterface();

    if (serviceInterfaceName == null) {
      return;
    }

    Bundle bundle = getComponentContext().getBundleContext().getBundle();
    ClassLoader classLoader = bundle.adapt(BundleWiring.class).getClassLoader();

    try {
      @SuppressWarnings("unchecked")
      Class<S> tmpServiceClass =
          (Class<S>) Class.forName(serviceInterfaceName, true, classLoader);
      this.serviceClass = tmpServiceClass;
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private void releaseServices() {
    Collection<SuitingWithService<S>> suitingsWithServices = previousSuitingsByRequirementId
        .values();

    for (SuitingWithService<S> suitingWithService : suitingsWithServices) {
      removeFromUsedServiceReferences(suitingWithService);
    }

    serviceObjectsByReferences.clear();
    previousSuitingsByRequirementId.clear();
  }

  private void removeFromUsedServiceReferences(final SuitingWithService<S> suitingWithService) {
    S service = suitingWithService.service;
    if (service != null) {
      ServiceReference<S> serviceReference = suitingWithService.suiting.getCapability();
      ServiceObjectsWithCounter<S> serviceObjectsWithCounter = serviceObjectsByReferences
          .get(serviceReference);
      serviceObjectsWithCounter.serviceObjects.ungetService(service);

      serviceObjectsWithCounter.counter--;

      if (serviceObjectsWithCounter.counter == 0) {
        serviceObjectsByReferences.remove(serviceReference);
      }
    }
  }

  private Object[] resolveParameterArray(final Suiting<ServiceReference<S>>[] tmpSuitings) {
    Object[] parameter;
    if (isHolder()) {
      parameter = new ServiceHolder[tmpSuitings.length];
    } else {
      parameter = (Object[]) Array.newInstance(serviceClass, tmpSuitings.length);
    }
    return parameter;
  }
}
