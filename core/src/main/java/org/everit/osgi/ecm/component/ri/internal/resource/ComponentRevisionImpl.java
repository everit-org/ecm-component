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
package org.everit.osgi.ecm.component.ri.internal.resource;

import java.io.Serializable;
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
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;

/**
 * Implementation of {@link ComponentRevision} that offers immutable information about the state of
 * a Component instance.
 *
 * @param <C>
 *          The type of the component implementation.
 */
public class ComponentRevisionImpl<C> implements ComponentRevision<C> {

  /**
   * The builder class of the {@link ComponentRevision} offers thread safe functionality to collect
   * information about the state of the component and build {@link ComponentRevision} instances.
   *
   * @param <C>
   *          The type of the component implementation.
   */
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

    /**
     * Called when the component instance becomes active.
     */
    public synchronized void active() {
      this.cache = null;
      this.state = ComponentState.ACTIVE;
      this.processingThread = null;

    }

    /**
     * Called when the component registers a new OSGi service that is shown as the capability of the
     * component.
     *
     * @param serviceRegistration
     *          The service that was registered.
     */
    public synchronized void addServiceRegistration(
        final ServiceRegistration<?> serviceRegistration) {

      cache = null;
      serviceRegistrations.add(serviceRegistration);
    }

    /**
     * Builds a new {@link ComponentRevision} based on the snapshot of the component state.
     *
     * @return The freshly created {@link ComponentRevision}.
     */
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

    /**
     * Called when the component fails.
     *
     * @param cause
     *          The cause of the failure.
     * @param permanent
     *          Whether the failure is permanent or temporary.
     */
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

    /**
     * Returns a clone of the held service registrations.
     *
     * @return A clone of the service registrations of the component.
     */
    public synchronized Set<ServiceRegistration<?>> getCloneOfServiceRegistrations() {
      @SuppressWarnings("unchecked")
      Set<ServiceRegistration<?>> result = (Set<ServiceRegistration<?>>) serviceRegistrations
          .clone();
      return result;
    }

    public synchronized Map<String, Object> getProperties() {
      return properties;
    }

    public synchronized ComponentState getState() {
      return state;
    }

    public synchronized void removeServiceRegistration(
        final ServiceRegistration<?> serviceRegistration) {
      cache = null;
      serviceRegistrations.remove(serviceRegistration);
    }

    /**
     * Called when the component is starting (activate method is called but it has not finished
     * yet).
     */
    public synchronized void starting() {
      this.cache = null;
      this.state = ComponentState.STARTING;
      this.cause = null;
      this.processingThread = Thread.currentThread();
    }

    /**
     * Called when the component is stopped due to some reason (it is stopped completely or the
     * component has to be restarted due to updating non-dynamic attributes).
     */
    public synchronized void stopped() {
      this.cache = null;
      this.processingThread = null;
      this.cause = null;
      this.state = ComponentState.STOPPED;
    }

    /**
     * Called when the component is stopping (The deactivate method runs).
     */
    public synchronized void stopping() {
      this.cache = null;
      this.state = ComponentState.STOPPING;
      this.processingThread = Thread.currentThread();
    }

    /**
     * Called when the component becomes unsatisfied.
     */
    public synchronized void unsatisfied() {
      this.cache = null;
      this.state = ComponentState.UNSATISFIED;
      this.processingThread = null;
      this.cause = null;
    }

    /**
     * Called when the component properties are updated.
     *
     * @param properties
     *          The new properties of the component.s
     */
    public synchronized void updateProperties(final Map<String, Object> properties) {
      this.cache = null;
      this.properties = properties;
    }

    public synchronized void updateSuitingsForAttribute(final ReferenceMetadata referenceMetadata,
        final Suiting<?>[] suitings) {
      this.cache = null;
      suitingsByAttributeIds.put(referenceMetadata, suitings);
    }

    /**
     * Called when the state is changed to {@link ComponentState#UPDATING_CONFIGURATION}.
     */
    public synchronized void updatingConfiguration() {
      this.cache = null;
      this.processingThread = Thread.currentThread();
      this.cause = null;
      this.state = ComponentState.UPDATING_CONFIGURATION;
    }
  }

  /**
   * Comparator that can be used to short requirements based on their ids.
   */
  private static final class ComponentRequirementComparator implements Comparator<Requirement>,
      Serializable {

    private static final long serialVersionUID = 6122207464125210506L;

    @Override
    public int compare(final Requirement o1, final Requirement o2) {
      return ((ComponentRequirement<?, ?>) o1).getRequirementId().compareTo(
          ((ComponentRequirement<?, ?>) o2).getRequirementId());
    }
  }

  /**
   * Holder class for Requirement and Wire mapping.
   */
  private static class RequirementsAndWires {
    public Map<String, List<Requirement>> requirements = new HashMap<String, List<Requirement>>();

    public List<Wire> wires = new ArrayList<Wire>();
  }

  private static final ComponentRequirementComparator REQUIREMENT_COMPARATOR =
      new ComponentRequirementComparator();

  private final Map<String, List<Capability>> capabilitiesByNamespace;

  private final Throwable cause;

  private final ComponentContainer<C> container;

  private final BundleRevision declaringResource;

  private final Thread processingThread;

  private final Map<String, Object> properties;

  private final Map<String, List<Requirement>> requirementsByNamespace;

  private final ComponentState state;

  private final Map<Capability, List<Wire>> wiresByCapability;

  private final Map<Requirement, List<Wire>> wiresByRequirement;

  /**
   * Constructor that should be called by the builder.
   *
   * @param builder
   *          The builder of the {@link ComponentRevisionImpl}.
   */
  protected ComponentRevisionImpl(final Builder<C> builder) {
    declaringResource = builder.container.getBundleContext().getBundle()
        .adapt(BundleRevision.class);
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

  private void addFilterToRequirementDefinitionIfExists(final Map<String, String> directives,
      final RequirementDefinition<?> requirementDefinition) {
    if (requirementDefinition.getFilter() != null) {
      directives.put("filter", requirementDefinition.getFilter().toString());
    }
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

  private String createFullRequirementId(final ReferenceMetadata referenceMetadata,
      final String referenceId,
      final Suiting<?> suiting) {
    String fullRequirementId = referenceId;
    if (referenceMetadata.isMultiple()
        || (referenceMetadata.getReferenceConfigurationType()
          == ReferenceConfigurationType.CLAUSE)) {

      fullRequirementId += "[" + suiting.getRequirement().getRequirementId() + "]";
    }
    return fullRequirementId;
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
    Iterator<Entry<ReferenceMetadata, Suiting<?>[]>> suitingEntryIterator =
        suitingEntries.iterator();

    RequirementsAndWires result = new RequirementsAndWires();

    while (suitingEntryIterator.hasNext()) {
      Map.Entry<ReferenceMetadata, Suiting<?>[]> entry = suitingEntryIterator.next();
      ReferenceMetadata referenceMetadata = entry.getKey();
      String referenceId = referenceMetadata.getReferenceId();
      String namespace = resolveNamespaceForWire(referenceMetadata);

      List<Requirement> requirementsOfNS = getOrCreateRequirementListOfNS(result, namespace);

      Suiting<?>[] suitings = entry.getValue();
      for (Suiting<?> suiting : suitings) {
        String fullRequirementId = createFullRequirementId(referenceMetadata, referenceId, suiting);

        Map<String, String> directives = new LinkedHashMap<String, String>();
        RequirementDefinition<?> requirementDefinition = suiting.getRequirement();
        if (referenceMetadata instanceof ServiceReferenceMetadata) {
          Class<?> serviceInterface = ((ServiceReferenceMetadata) referenceMetadata)
              .getServiceInterface();
          if (serviceInterface != null) {
            directives.put(Constants.OBJECTCLASS, serviceInterface.getName());
          }
        }

        addFilterToRequirementDefinitionIfExists(directives, requirementDefinition);

        Class<? extends Capability> capabilityType = specifyCapabilityType(referenceMetadata);

        @SuppressWarnings("unchecked")
        Class<Capability> simpleCapabilityType = (Class<Capability>) capabilityType;

        HashMap<String, Object> attributes = new LinkedHashMap<String, Object>(
            requirementDefinition.getAttributes());

        ComponentRequirementImpl<C, Capability> requirement =
            new ComponentRequirementImpl<C, Capability>(
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
  public BundleRevision getDeclaringResource() {
    return declaringResource;
  }

  private List<Requirement> getOrCreateRequirementListOfNS(final RequirementsAndWires result,
      final String namespace) {
    List<Requirement> requirementsOfNS = result.requirements.get(namespace);
    if (requirementsOfNS == null) {
      requirementsOfNS = new ArrayList<Requirement>();
      result.requirements.put(namespace, requirementsOfNS);
    }
    return requirementsOfNS;
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

  /**
   * Returns the wires that belong to and known by the component this {@link ComponentRevision}
   * belongs to.
   *
   * @return The wires created by the component this {@link ComponentRevision} belongs to.
   */
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

  /**
   * Finds {@link Wire}s by the {@link Capability} they wires to.
   *
   * @param capability
   *          The {@link Capability} of the {@link Wire}s.
   * @return A list of {@link Wire}s that are wired to the specified {@link Capability}.
   */
  public List<Wire> getWiresByCapability(final Capability capability) {
    List<Wire> wireList = wiresByCapability.get(capability);
    if (wireList == null) {
      return Collections.emptyList();
    }
    return wireList;
  }

  /**
   * Finds {@link Wire}s by requirements they wire to.
   *
   * @param requirement
   *          The {@link Requirement} that {@link Wire#getRequirement()} would return.
   * @return The {@link Wire} which returns the specified {@link Requirement} in case of calling
   *         {@link Wire#getRequirement()}.
   */
  public List<Wire> getWiresByRequirement(final Requirement requirement) {
    List<Wire> wireList = wiresByRequirement.get(requirement);
    if (wireList == null) {
      return Collections.emptyList();
    }
    return wireList;
  }

  private String resolveNamespaceForWire(final ReferenceMetadata referenceMetadata) {
    String namespace = ServiceCapability.SERVICE_CAPABILITY_NAMESPACE;
    if (referenceMetadata instanceof BundleCapabilityReferenceMetadata) {
      namespace = ((BundleCapabilityReferenceMetadata) referenceMetadata).getNamespace();
    }
    return namespace;
  }

  private Class<? extends Capability> specifyCapabilityType(
      final ReferenceMetadata referenceMetadata) {
    Class<? extends Capability> capabilityType;
    if (referenceMetadata instanceof ServiceReferenceMetadata) {
      capabilityType = ServiceCapability.class;
    } else {
      capabilityType = BundleCapability.class;
    }
    return capabilityType;
  }
}
