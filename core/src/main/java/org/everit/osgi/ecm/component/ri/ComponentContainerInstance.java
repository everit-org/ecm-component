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

import org.everit.osgi.ecm.component.resource.ComponentContainer;

/**
 * A representation of an instance of a {@link ComponentContainer}. This interface should be used
 * only by the creator of the {@link ComponentContainer}.
 *
 * @param <C>
 *          The type of the components managed by this container.
 */
public interface ComponentContainerInstance<C> extends ComponentContainer<C> {

  /**
   * Stops and deletes all components that are managed by this container and unregisters registered
   * OSGi services.
   */
  void close();

  /**
   * Starts the component container instance. After starting the container, the followings will
   * happen:
   * <ul>
   * <li>registering an OSGi service based on {@link ComponentContainer} and optionally
   * {@link org.osgi.service.metatype.MetaTypeProvider} and
   * {@link org.osgi.service.cm.ManagedService} or {@link org.osgi.service.cm.ManagedServiceFactory}
   * interfaces</li>
   * <li>Starting and stopping Components based on the metadata information and configuration
   * changes</li>
   * </ul>
   */
  void open();
}
