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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.everit.osgi.ecm.component.ECMComponentConstants;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.component.ri.internal.metatype.MetatypeProviderImpl;
import org.everit.osgi.ecm.component.ri.internal.resource.ComponentRevisionImpl;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Class that holds common functionality of Component containers with 0..1 and containers with 0..n
 * (factory) configuration cardinality.
 *
 * @param <C>
 *          The type of the component instance.
 */
public abstract class AbstractComponentContainer<C> implements MetaTypeProvider,
    ComponentContainerInstance<C> {

  private final BundleContext bundleContext;

  private final ComponentMetadata componentMetadata;

  private final MetatypeProviderImpl<C> metatypeProvider;

  /**
   * Constructor.
   *
   * @param componentMetadata
   *          The metadata information of the components that should be managed by this container.
   * @param bundleContext
   *          The context of the bundle that implemented the component.
   */
  public AbstractComponentContainer(final ComponentMetadata componentMetadata,
      final BundleContext bundleContext) {
    this.componentMetadata = componentMetadata;
    this.bundleContext = bundleContext;
    this.metatypeProvider = new MetatypeProviderImpl<C>(componentMetadata, bundleContext);
  }

  /**
   * Add service properties that are available for all kind of components:
   * {@value ECMComponentConstants#SERVICE_PROP_COMPONENT_NAME} and
   * {@value ECMComponentConstants#SERVICE_PROP_COMPONENT_CLASS}.
   *
   * @param properties
   *          The configuration of the component.
   */
  protected void addCommonServiceProperties(final Dictionary<String, Object> properties) {
    properties.put(ECMComponentConstants.SERVICE_PROP_COMPONENT_CLASS, componentMetadata.getType());
    properties.put(ECMComponentConstants.SERVICE_PROP_COMPONENT_NAME, this.metatypeProvider
        .getObjectClassDefinition(null, null).getName());
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
  public String[] getLocales() {
    return metatypeProvider.getLocales();
  }

  @Override
  public ObjectClassDefinition getObjectClassDefinition(final String id, final String locale) {
    return metatypeProvider.getObjectClassDefinition(id, locale);
  }

  @Override
  public abstract ComponentRevisionImpl<C>[] getResources();

  @Override
  public synchronized Wire[] getWires() {
    ComponentRevisionImpl<C>[] componentRevisions = getResources();
    if (componentRevisions.length == 0) {
      return new Wire[0];
    }
    if (componentRevisions.length == 1) {
      List<Wire> wires = componentRevisions[0].getWires();
      return wires.toArray(new Wire[wires.size()]);
    }

    List<Wire> result = new ArrayList<Wire>();
    for (ComponentRevisionImpl<C> componentRevisionImpl : componentRevisions) {
      result.addAll(componentRevisionImpl.getWires());
    }
    return result.toArray(new Wire[result.size()]);
  }

  @Override
  public Wire[] getWiresByCapability(final Capability capability) {
    ComponentRevisionImpl<C>[] componentRevisions = getResources();
    if (componentRevisions.length == 0) {
      return new Wire[0];
    }
    if (componentRevisions.length == 1) {
      List<Wire> wires = componentRevisions[0].getWiresByCapability(capability);
      return wires.toArray(new Wire[wires.size()]);
    }

    List<Wire> result = new ArrayList<Wire>();
    for (ComponentRevisionImpl<C> componentRevisionImpl : componentRevisions) {
      result.addAll(componentRevisionImpl.getWiresByCapability(capability));
    }
    return result.toArray(new Wire[result.size()]);
  }

  @Override
  public Wire[] getWiresByRequirement(final Requirement requirement) {
    ComponentRevisionImpl<C>[] componentRevisions = getResources();
    if (componentRevisions.length == 0) {
      return new Wire[0];
    }
    if (componentRevisions.length == 1) {
      List<Wire> wires = componentRevisions[0].getWiresByRequirement(requirement);
      return wires.toArray(new Wire[wires.size()]);
    }

    List<Wire> result = new ArrayList<Wire>();
    for (ComponentRevisionImpl<C> componentRevisionImpl : componentRevisions) {
      result.addAll(componentRevisionImpl.getWiresByRequirement(requirement));
    }
    return result.toArray(new Wire[result.size()]);
  }
}
