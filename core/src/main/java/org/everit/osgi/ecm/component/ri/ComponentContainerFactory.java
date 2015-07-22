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
package org.everit.osgi.ecm.component.ri;

import org.everit.osgi.ecm.component.ri.internal.ComponentContainerImpl;
import org.everit.osgi.ecm.component.ri.internal.FactoryComponentContainerImpl;
import org.everit.osgi.ecm.component.ri.internal.JavaLogService;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.ConfigurationPolicy;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Factory class to create {@link org.everit.osgi.ecm.component.resource.ComponentContainer}s.
 */
public class ComponentContainerFactory {

  private final BundleContext bundleContext;

  private final LogService logService;

  /**
   * Same as using the constructor ComponentcontainerFactory(bundleContext, null).
   *
   * @param bundleContext
   *          The context of the bundle that would like to create and open
   *          {@link ComponentContainerInstance}s.
   */
  public ComponentContainerFactory(final BundleContext bundleContex) {
    this(bundleContex, null);
  }

  /**
   * Constructor.
   *
   * @param bundleContext
   *          The context of the bundle that would like to create and open
   *          {@link ComponentContainerInstance}s.
   * @param logService
   *          The logService to log to or <code>null</code> if the standard JDK logger should be
   *          used.
   */
  public ComponentContainerFactory(final BundleContext bundleContext, final LogService logService) {
    this.bundleContext = bundleContext;
    if (logService != null) {
      this.logService = logService;
    } else {
      this.logService = new JavaLogService();
    }
  }

  /**
   * Creates a new {@link ComponentContainerInstance}.
   *
   * @param componentMetadata
   *          The metadata of the component(s) that should be instantiated by the container.
   * @return The component container instance that manages the lifecycle of the components. The
   *         instance is registered as an OSGi service after it is opened.
   */
  public <C> ComponentContainerInstance<C> createComponentContainer(
      final ComponentMetadata componentMetadata) {
    if (ConfigurationPolicy.FACTORY.equals(componentMetadata.getConfigurationPolicy())) {
      return new FactoryComponentContainerImpl<C>(componentMetadata, bundleContext, logService);
    } else {
      return new ComponentContainerImpl<C>(componentMetadata, bundleContext, logService);
    }
  }
}
