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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.ri.internal.resource.ComponentRevisionImpl;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.ConfigurationPolicy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeProvider;

/**
 * Implementation of non-factory {@link ComponentContainer} that can ignore or accept one optional
 * or required configuration for instantiation.
 *
 * @param <C>
 *          The type of the implementation.
 */
public class ComponentContainerImpl<C> extends AbstractComponentContainer<C>
    implements ManagedService {

  private AtomicBoolean closed = new AtomicBoolean(true);

  private final AtomicReference<ComponentContextImpl<C>> componentAtomicReference =
      new AtomicReference<ComponentContextImpl<C>>();

  public ComponentContainerImpl(final ComponentMetadata componentMetadata,
      final BundleContext bundleContext, final LogService logService) {
    super(componentMetadata, bundleContext, logService);
  }

  @Override
  public void close() {
    boolean closing = closed.compareAndSet(false, true);
    if (closing) {
      ComponentContextImpl<C> componentImpl = componentAtomicReference.get();
      if (componentImpl != null) {
        componentImpl.close();
        componentAtomicReference.set(null);
      }
    }
    unregisterService();
  }

  @Override
  public ComponentRevisionImpl<C>[] getResources() {
    ComponentContextImpl<C> componentImpl = componentAtomicReference.get();
    if (componentImpl == null) {
      @SuppressWarnings("unchecked")
      ComponentRevisionImpl<C>[] result = new ComponentRevisionImpl[0];
      return result;
    }
    @SuppressWarnings("unchecked")
    ComponentRevisionImpl<C>[] result = new ComponentRevisionImpl[] { componentImpl
        .getComponentRevision() };
    return result;
  }

  @Override
  public void open() {
    closed.set(false);
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    List<String> serviceInterfaces = new LinkedList<String>();
    serviceInterfaces.add(ComponentContainer.class.getName());

    ComponentMetadata componentMetadata = getComponentMetadata();

    addCommonServiceProperties(properties);

    if (!ConfigurationPolicy.IGNORE.equals(componentMetadata.getConfigurationPolicy())) {
      if (componentMetadata.isMetatype()) {
        properties.put(MetaTypeProvider.METATYPE_PID, componentMetadata.getConfigurationPid());
        serviceInterfaces.add(MetaTypeProvider.class.getName());
      }
      properties.put(Constants.SERVICE_PID, componentMetadata.getConfigurationPid());
      serviceInterfaces.add(ManagedService.class.getName());
    }

    registerService(properties, serviceInterfaces);

    ComponentContextImpl<C> componentImpl = componentAtomicReference.get();
    if (ConfigurationPolicy.IGNORE.equals(componentMetadata.getConfigurationPolicy())
        && (componentImpl == null)) {
      componentImpl = new ComponentContextImpl<C>(this, getBundleContext(), null, getLogService());
      componentAtomicReference.set(componentImpl);
      componentImpl.open();
      return;
    }

  }

  @Override
  public synchronized void updated(final Dictionary<String, ?> properties)
      throws ConfigurationException {
    @SuppressWarnings("unchecked")
    Dictionary<String, Object> props = (Dictionary<String, Object>) properties;

    ComponentMetadata componentMetadata = getComponentMetadata();
    ComponentContextImpl<C> componentImpl = componentAtomicReference.get();

    ConfigurationPolicy configurationPolicy = componentMetadata.getConfigurationPolicy();

    if ((componentImpl == null)
        && ((properties != null) || ConfigurationPolicy.OPTIONAL.equals(configurationPolicy))) {
      componentImpl = new ComponentContextImpl<C>(this, getBundleContext(), props, getLogService());
      componentAtomicReference.set(componentImpl);
      componentImpl.open();
    } else if ((componentImpl != null) && (properties == null)
        && !ConfigurationPolicy.OPTIONAL.equals(configurationPolicy)) {

      if (!closed.get()) {
        componentImpl = componentAtomicReference.getAndSet(null);
        if (componentImpl != null) {
          componentImpl.close();
        }
      }

    } else if (componentImpl != null) {
      @SuppressWarnings("unchecked")
      Dictionary<String, Object> propertiesWithObjectGenerics =
          (Dictionary<String, Object>) properties;

      componentImpl.updateConfiguration(propertiesWithObjectGenerics);
    }
  }
}
