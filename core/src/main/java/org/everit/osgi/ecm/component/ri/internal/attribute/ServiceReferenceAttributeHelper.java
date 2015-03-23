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
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.everit.osgi.capabilitycollector.AbstractCapabilityCollector;
import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.everit.osgi.capabilitycollector.ServiceReferenceCollector;
import org.everit.osgi.capabilitycollector.Suiting;
import org.everit.osgi.ecm.component.ServiceHolder;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.component.ri.internal.ComponentContextImpl;
import org.everit.osgi.ecm.component.ri.internal.ReferenceEventHandler;
import org.everit.osgi.ecm.metadata.ServiceReferenceMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

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
   * Mapping of {@link Suiting}s and the requested OSGi service instance (suiting contain only
   * service reference).
   *
   * @param <S>
   *          The of the OSGi service.
   */
  private static class SuitingWithService<S> {
    public S service;

    public Suiting<ServiceReference<S>> suiting;
  }

  private Map<String, SuitingWithService<S>> previousSuitingsByRequirementId = new TreeMap<>();

  private final Map<ServiceReference<S>, Integer> usedServiceReferences = new HashMap<>();

  public ServiceReferenceAttributeHelper(final ServiceReferenceMetadata referenceMetadata,
      final ComponentContextImpl<COMPONENT> componentContext,
      final ReferenceEventHandler eventHandler)
      throws IllegalAccessException {
    super(referenceMetadata, componentContext, eventHandler);
  }

  private void addToUsedServiceReferences(final ServiceReference<S> serviceReference) {
    Integer count = usedServiceReferences.get(serviceReference);
    if (count == null) {
      count = 0;
    }
    count = count + 1;
    usedServiceReferences.put(serviceReference, count);
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

      String requirementId = suiting
          .getRequirement().getRequirementId();
      SuitingWithService<S> previousSuitingWithService = previousSuitingsByRequirementId
          .get(requirementId);

      S service;
      if ((previousSuitingWithService == null)
          || (previousSuitingWithService.suiting.getCapability()
              .compareTo(suiting.getCapability()) != 0)) {

        BundleContext bundleContext = getComponentContext().getBundleContext();
        service = bundleContext.getService(serviceReference);
        addToUsedServiceReferences(serviceReference);
        if (service != null) {
          SuitingWithService<S> suitingWithService = new SuitingWithService<S>();
          suitingWithService.service = service;
          suitingWithService.suiting = suiting;
          newSuitingMapping.put(requirementId, suitingWithService);
        }
      } else {
        previousSuitingsByRequirementId.remove(requirementId);
        service = previousSuitingWithService.service;
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
    if (getComponentContext().getState() == ComponentState.FAILED) {
      return;
    }

    Collection<SuitingWithService<S>> previousSuitings = previousSuitingsByRequirementId.values();
    for (SuitingWithService<S> suitingWithService : previousSuitings) {
      ServiceReference<S> serviceReference = suitingWithService.suiting.getCapability();
      removeFromUsedServiceReferences(serviceReference);
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

    Set<Entry<ServiceReference<S>, Integer>> usedServiceReferenceEntries = usedServiceReferences
        .entrySet();
    BundleContext bundleContext = getComponentContext().getBundleContext();
    for (Entry<ServiceReference<S>, Integer> entry : usedServiceReferenceEntries) {
      ServiceReference<S> serviceReference = entry.getKey();
      Integer count = entry.getValue();
      for (int i = 0, n = count.intValue(); i < n; i++) {
        bundleContext.ungetService(serviceReference);
      }
    }
    usedServiceReferences.clear();
    previousSuitingsByRequirementId.clear();
  }

  @Override
  protected AbstractCapabilityCollector<ServiceReference<S>> createCollector(
      final ReferenceCapabilityConsumer consumer,
      final RequirementDefinition<ServiceReference<S>>[] items) {

    @SuppressWarnings("unchecked")
    Class<S> serviceInterface = (Class<S>) getReferenceMetadata().getServiceInterface();
    return new ServiceReferenceCollector<S>(getComponentContext().getBundleContext(),
        serviceInterface, items, consumer, false);
  }

  private void removeFromUsedServiceReferences(final ServiceReference<S> serviceReference) {
    Integer count = usedServiceReferences.get(serviceReference);
    if (count == null) {
      return;
    }
    count = count - 1;
    if (count.intValue() == 0) {
      getComponentContext().getBundleContext().ungetService(serviceReference);
      usedServiceReferences.remove(serviceReference);
    } else {
      usedServiceReferences.put(serviceReference, count);
    }
  }

  private Object[] resolveParameterArray(final Suiting<ServiceReference<S>>[] tmpSuitings) {
    Object[] parameter;
    if (isHolder()) {
      parameter = new ServiceHolder[tmpSuitings.length];
    } else {
      parameter = (Object[]) Array.newInstance(getReferenceMetadata().getServiceInterface(),
          tmpSuitings.length);
    }
    return parameter;
  }
}
