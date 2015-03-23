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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.ri.internal.resource.ComponentRevisionImpl;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.MetaTypeProvider;

/**
 * {@link ComponentContainer} for components that are instantiated multiple times based on the
 * number of configurations.
 *
 * @param <C>
 *          The type of the component implementation.
 */
public class FactoryComponentContainerImpl<C> extends AbstractComponentContainer<C> implements
    ManagedServiceFactory {

  private final Map<String, ComponentContextImpl<C>> components =
      new ConcurrentHashMap<String, ComponentContextImpl<C>>();

  private ServiceRegistration<?> serviceRegistration;

  public FactoryComponentContainerImpl(final ComponentMetadata componentMetadata,
      final BundleContext bundleContext) {
    super(componentMetadata, bundleContext);
  }

  @Override
  public void close() {
    Iterator<Entry<String, ComponentContextImpl<C>>> iterator = components.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<java.lang.String, ComponentContextImpl<C>> entry = iterator
          .next();

      ComponentContextImpl<?> componentContextImpl = entry.getValue();
      componentContextImpl.close();
      iterator.remove();
    }

    if (serviceRegistration != null) {
      serviceRegistration.unregister();
      serviceRegistration = null;
    }
  }

  @Override
  public void deleted(final String pid) {
    ComponentContextImpl<?> component = components.get(pid);
    if (component != null) {
      component.close();
      components.remove(pid);
    }
  }

  @Override
  public String getName() {
    return getComponentMetadata().getComponentId();
  }

  @Override
  public ComponentRevisionImpl<C>[] getResources() {
    Collection<ComponentContextImpl<C>> values = components.values();

    @SuppressWarnings("unchecked")
    ComponentRevisionImpl<C>[] result = new ComponentRevisionImpl[values.size()];

    int i = 0;
    for (ComponentContextImpl<C> componentContextImpl : values) {
      result[i] = componentContextImpl.getComponentRevision();
      i++;
    }

    return result;
  }

  @Override
  public void open() {
    BundleContext context = getBundleContext();
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    List<String> serviceInterfaces = new LinkedList<String>();
    serviceInterfaces.add(ComponentContainer.class.getName());

    ComponentMetadata componentMetadata = getComponentMetadata();
    if (componentMetadata.isMetatype()) {
      properties
          .put(MetaTypeProvider.METATYPE_FACTORY_PID, componentMetadata.getConfigurationPid());
      serviceInterfaces.add(MetaTypeProvider.class.getName());
    }
    addCommonServiceProperties(properties);

    properties.put(Constants.SERVICE_PID, componentMetadata.getConfigurationPid());
    serviceInterfaces.add(ManagedServiceFactory.class.getName());

    serviceRegistration = context.registerService(
        serviceInterfaces.toArray(new String[serviceInterfaces.size()]),
        this, properties);

  }

  @Override
  public void updated(final String pid, final Dictionary<String, ?> properties)
      throws ConfigurationException {
    @SuppressWarnings("unchecked")
    Dictionary<String, Object> props = (Dictionary<String, Object>) properties;

    ComponentContextImpl<?> componentContextImpl = components.get(pid);
    if (componentContextImpl != null) {
      componentContextImpl.updateConfiguration(properties);
    } else {
      ComponentContextImpl<C> newComponent = new ComponentContextImpl<C>(this, getBundleContext(),
          props);
      components.put(pid, newComponent);
      newComponent.open();
    }
  }
}
