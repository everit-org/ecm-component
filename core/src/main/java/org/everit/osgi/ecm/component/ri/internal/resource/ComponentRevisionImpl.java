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
package org.everit.osgi.ecm.component.ri.internal.resource;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.everit.osgi.capabilitycollector.Suiting;
import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.resource.ComponentRequirement;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.metadata.BundleCapabilityReferenceMetadata;
import org.everit.osgi.ecm.metadata.ReferenceConfigurationType;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;
import org.everit.osgi.ecm.metadata.ServiceReferenceMetadata;
import org.everit.osgi.linkage.ServiceCapability;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;

public class ComponentRevisionImpl<C> implements ComponentRevision<C> {

  public static class Builder<C> {

    private SoftReference<ComponentRevisionImpl<C>> cache = null;

    private Throwable cause = null;

    private final ComponentContainer<C> container;

    private Thread processingThread = null;

    private Map<String, Object> properties;

    private final LinkedHashSet<ServiceRegistration<?>> serviceRegistrations =
        new LinkedHashSet<ServiceRegistration<?>>();

    private ComponentState state = ComponentState.STOPPED;

    private final Map<ReferenceMetadata, Suiting<?>[]> suitingsByAttributeIds =
        new HashMap<ReferenceMetadata, Suiting<?>[]>();

    public Builder(final ComponentContainer<C> container, final Map<String, Object> properties) {
      this.container = container;
      this.properties = properties;
    }

    public synchronized void active() {
      this.cache = null;
      this.state = ComponentState.ACTIVE;
      this.processingThread = null;

    }

    public synchronized void addServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
      cache = null;
      serviceRegistrations.add(serviceRegistration);
    }

    public synchronized ComponentRevisionImpl<C> build() {
      if (cache != null) {
        ComponentRevisionImpl<C> componentRevisionImpl = cache.get();
        if (componentRevisionImpl != null) {
          return componentRevisionImpl;
        }
      }
      ComponentRevisionImpl<C> componentRevisionImpl = new ComponentRevisionImpl<C>(this);
      cache = new SoftReference<ComponentRevisionImpl<C>>(componentRevisionImpl);
      return componentRevisionImpl;

    }

    public synchronized void fail(final Throwable cause, final boolean permanent) {
      this.cache = null;
      this.cause = cause;
      this.processingThread = null;
      if (permanent) {
        this.state = ComponentState.FAILED_PERMANENT;
      } else {
        this.state = ComponentState.FAILED;
      }
    }

    public Set<ServiceRegistration<?>> getCloneOfServiceRegistrations() {
      @SuppressWarnings("unchecked")
      Set<ServiceRegistration<?>> result = (Set<ServiceRegistration<?>>) serviceRegistrations
          .clone();
      return result;
    }

    public Map<String, Object> getProperties() {
      // TODO do not give this out only if it is immutable
      return properties;
    }

    public ComponentState getState() {
      return state;
    }

    public synchronized void removeServiceRegistration(
        final ServiceRegistration<?> serviceRegistration) {
      cache = null;
      serviceRegistrations.remove(serviceRegistration);
    }

    public synchronized void starting() {
      this.cache = null;
      this.state = ComponentState.STARTING;
      this.cause = null;
      this.processingThread = Thread.currentThread();
    }

    public synchronized void stopped(final ComponentState targetState) {
      this.cache = null;
      if (targetState != ComponentState.UPDATING_CONFIGURATION) {
        this.processingThread = null;
      }
      if ((targetState == ComponentState.STOPPED)
          || (targetState == ComponentState.UPDATING_CONFIGURATION)) {
        cause = null;
      }
      state = targetState;
    }

    public synchronized void stopping() {
      this.cache = null;
      this.state = ComponentState.STOPPING;
      this.processingThread = Thread.currentThread();
    }

    public synchronized void unsatisfied() {
      this.cache = null;
      this.state = ComponentState.UNSATISFIED;
      this.processingThread = null;
      this.cause = null;
    }

    public synchronized void updateProperties(final Map<String, Object> properties) {
      this.cache = null;
      this.properties = properties;
      this.state = ComponentState.UPDATING_CONFIGURATION;
    }

    public synchronized void updateSuitingsForAttribute(final ReferenceMetadata referenceMetadata,
        final Suiting<?>[] suitings) {
      this.cache = null;
      suitingsByAttributeIds.put(referenceMetadata, suitings);
    }
  }

  private static final class ComponentRequirementComparator implements Comparator<Requirement> {
    @Override
    public int compare(final Requirement o1, final Requirement o2) {
      return ((ComponentRequirement<?>) o1).getRequirementId().compareTo(
          ((ComponentRequirement<?>) o2).getRequirementId());
    }
  }

  private static class RequirementsAndWires {
    public Map<String, List<Requirement>> requirements = new HashMap<String, List<Requirement>>();
    public List<Wire> wires = new ArrayList<Wire>();
  }

  private static final ComponentRequirementComparator REQUIREMENT_COMPARATOR = new ComponentRequirementComparator();

  private final Map<String, List<Capability>> capabilitiesByNamespace;

  private final Throwable cause;

  private final ComponentContainer<C> container;

  private final Thread processingThread;

  private final Map<String, Object> properties;

  private final Map<String, List<Requirement>> requirementsByNamespace;

  private final ComponentState state;

  private final Map<Capability, List<Wire>> wiresByCapability;

  private final Map<Requirement, List<Wire>> wiresByRequirement;

  private ComponentRevisionImpl(final Builder<C> builder) {
    this.state = builder.state;
    this.processingThread = builder.processingThread;
    this.cause = builder.cause;
    this.properties = builder.properties;
    this.container = builder.container;

    this.capabilitiesByNamespace = evaluateCapabilities(builder);

    RequirementsAndWires requirementsAndWires = evaluateRequirementsAndWires(builder);

    // Making all set readonly
    for (Entry<String, List<Requirement>> entry : requirementsAndWires.requirements.entrySet()) {
      Collections.sort(entry.getValue(), REQUIREMENT_COMPARATOR);
      entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }
    this.requirementsByNamespace = Collections.unmodifiableMap(requirementsAndWires.requirements);

    Map<Capability, List<Wire>> lWiresByCapability = new HashMap<Capability, List<Wire>>();
    Map<Requirement, List<Wire>> lWiresByRequirement = new HashMap<Requirement, List<Wire>>();
    List<Wire> wires = requirementsAndWires.wires;
    for (Wire wire : wires) {
      Capability capability = wire.getCapability();
      Requirement requirement = wire.getRequirement();

      addToWireMap(lWiresByCapability, capability, wire);
      addToWireMap(lWiresByRequirement, requirement, wire);
    }
    this.wiresByCapability = convertToUnmodifiableWireMap(lWiresByCapability);
    this.wiresByRequirement = convertToUnmodifiableWireMap(lWiresByRequirement);
  }

  private <CONNECTOR> void addToWireMap(final Map<CONNECTOR, List<Wire>> map,
      final CONNECTOR connector,
      final Wire wire) {

    List<Wire> wireList = map.get(connector);
    if (wireList == null) {
      wireList = new ArrayList<Wire>();
      map.put(connector, wireList);
    }
    wireList.add(wire);
  }

  private <CONNECTOR> Map<CONNECTOR, List<Wire>> convertToUnmodifiableWireMap(
      final Map<CONNECTOR, List<Wire>> map) {
    Set<Entry<CONNECTOR, List<Wire>>> entrySet = map.entrySet();
    Iterator<Entry<CONNECTOR, List<Wire>>> iterator = entrySet.iterator();
    while (iterator.hasNext()) {
      Map.Entry<CONNECTOR, List<Wire>> entry = iterator.next();
      entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }
    return Collections.unmodifiableMap(map);
  }

  private Map<String, List<Capability>> evaluateCapabilities(final Builder<C> builder) {
    if (((this.state != ComponentState.ACTIVE) && (this.state != ComponentState.UNSATISFIED)
        && (this.state != ComponentState.FAILED)) || (builder.serviceRegistrations.size() == 0)) {
      return Collections.emptyMap();
    }

    Map<String, List<Capability>> result = new LinkedHashMap<String, List<Capability>>();

    List<Capability> serviceCapabilityList = new ArrayList<Capability>(
        builder.serviceRegistrations.size());

    Iterator<ServiceRegistration<?>> iterator = builder.serviceRegistrations.iterator();
    while (iterator.hasNext()) {
      ServiceRegistration<?> serviceRegistration = iterator.next();
      serviceCapabilityList.add(new ServiceCapabilityImpl(serviceRegistration.getReference()));
    }

    result.put("osgi.service", Collections.unmodifiableList(serviceCapabilityList));

    return Collections.unmodifiableMap(result);
  }

  private RequirementsAndWires evaluateRequirementsAndWires(final Builder<C> builder) {
    if ((this.state != ComponentState.ACTIVE) && (this.state != ComponentState.UNSATISFIED)
        && (this.state != ComponentState.FAILED)) {
      return new RequirementsAndWires();
    }

    Set<Entry<ReferenceMetadata, Suiting<?>[]>> suitingEntries = builder.suitingsByAttributeIds
        .entrySet();
    Iterator<Entry<ReferenceMetadata, Suiting<?>[]>> iterator = suitingEntries.iterator();

    RequirementsAndWires result = new RequirementsAndWires();

    while (iterator.hasNext()) {
      Map.Entry<ReferenceMetadata, Suiting<?>[]> entry = iterator.next();
      ReferenceMetadata referenceMetadata = entry.getKey();
      String referenceId = referenceMetadata.getReferenceId();
      String namespace = ServiceCapability.SERVICE_CAPABILITY_NAMESPACE;
      if (referenceMetadata instanceof BundleCapabilityReferenceMetadata) {
        namespace = ((BundleCapabilityReferenceMetadata) referenceMetadata).getNamespace();
      }

      List<Requirement> requirementsOfNS = result.requirements.get(namespace);
      if (requirementsOfNS == null) {
        requirementsOfNS = new ArrayList<Requirement>();
        result.requirements.put(namespace, requirementsOfNS);
      }

      Suiting<?>[] suitings = entry.getValue();
      for (Suiting<?> suiting : suitings) {
        String fullRequirementId = referenceId;
        if (referenceMetadata.isMultiple()
            || (referenceMetadata.getReferenceConfigurationType() == ReferenceConfigurationType.CLAUSE)) {

          fullRequirementId += "[" + suiting.getRequirement().getRequirementId() + "]";
        }

        Map<String, String> directives = new LinkedHashMap<String, String>();
        RequirementDefinition<?> requirementDefinition = suiting.getRequirement();
        if (referenceMetadata instanceof ServiceReferenceMetadata) {
          Class<?> serviceInterface = ((ServiceReferenceMetadata) referenceMetadata)
              .getServiceInterface();
          directives.put(Constants.OBJECTCLASS, serviceInterface.getName());
        }

        if (requirementDefinition.getFilter() != null) {
          directives.put("filter", requirementDefinition.getFilter().toString());
        }

        Class<? extends Capability> capabilityType;
        if (referenceMetadata instanceof ServiceReferenceMetadata) {
          capabilityType = ServiceCapability.class;
        } else {
          capabilityType = BundleCapability.class;
        }

        @SuppressWarnings("unchecked")
        Class<Capability> simpleCapabilityType = (Class<Capability>) capabilityType;

        HashMap<String, Object> attributes = new LinkedHashMap<String, Object>(
            requirementDefinition.getAttributes());

        ComponentRequirementImpl<Capability> requirement = new ComponentRequirementImpl<Capability>(
            fullRequirementId, namespace, this, Collections.unmodifiableMap(directives),
            Collections.unmodifiableMap(attributes), simpleCapabilityType);

        requirementsOfNS.add(requirement);

        Object capabilityObject = suiting.getCapability();
        Capability capability = null;
        if (capabilityObject != null) {
          if (capabilityObject instanceof ServiceReference<?>) {
            capability = new ServiceCapabilityImpl((ServiceReference<?>) capabilityObject);
          } else {
            // This must be BundleCapability than
            capability = (BundleCapability) capabilityObject;
          }

          result.wires.add(new ComponentWireImpl(requirement, capability));
        }

      }
    }

    return result;
  }

  @Override
  public List<Capability> getCapabilities(final String namespace) {
    return getConnectorsByNamespace(namespace, capabilitiesByNamespace);
  }

  @Override
  public Throwable getCause() {
    return cause;
  }

  @Override
  public ComponentContainer<C> getComponentContainer() {
    return this.container;
  }

  private <W> List<W> getConnectorsByNamespace(final String namespace,
      final Map<String, List<W>> wiringsByNamespace) {

    List<W> result;
    if (namespace != null) {
      result = wiringsByNamespace.get(namespace);
      if (result == null) {
        result = Collections.emptyList();
      }
    } else {
      result = new ArrayList<W>();
      Collection<List<W>> values = wiringsByNamespace.values();
      for (List<W> requirements : values) {
        result.addAll(requirements);
      }
      result = Collections.unmodifiableList(result);
    }
    return result;
  }

  @Override
  public Thread getProcessingThread() {
    return processingThread;
  }

  @Override
  public Map<String, Object> getProperties() {
    return properties;
  }

  @Override
  public List<Requirement> getRequirements(final String namespace) {
    return getConnectorsByNamespace(namespace, requirementsByNamespace);
  }

  @Override
  public ComponentState getState() {
    return state;
  }

  public List<Wire> getWires() {
    Set<Entry<Capability, List<Wire>>> entrySet = wiresByCapability.entrySet();
    if (entrySet.size() == 0) {
      return Collections.emptyList();
    }
    Iterator<Entry<Capability, List<Wire>>> iterator = entrySet.iterator();
    if (entrySet.size() == 1) {
      return iterator.next().getValue();
    }

    List<Wire> result = new ArrayList<Wire>();
    while (iterator.hasNext()) {
      Map.Entry<Capability, List<Wire>> entry = iterator.next();
      result.addAll(entry.getValue());
    }
    return result;
  }

  public List<Wire> getWiresByCapability(final Capability capability) {
    List<Wire> wireList = wiresByCapability.get(capability);
    if (wireList == null) {
      return Collections.emptyList();
    }
    return wireList;
  }

  public List<Wire> getWiresByRequirement(final Requirement requirement) {
    List<Wire> wireList = wiresByRequirement.get(requirement);
    if (wireList == null) {
      return Collections.emptyList();
    }
    return wireList;
  }
}
