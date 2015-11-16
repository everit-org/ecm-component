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

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Helper class to handle OSGi services registered in the component instance programmatically.
 *
 * @param <S>
 *          The type of the OSGi service.
 * @param <C>
 *          The type of the component implementation.
 */
class ComponentServiceRegistration<S, C> implements ServiceRegistration<S> {

  private final ComponentContextImpl<C> component;

  private final ServiceRegistration<S> wrapped;

  ComponentServiceRegistration(final ComponentContextImpl<C> component,
      final ServiceRegistration<S> wrapped) {
    this.component = component;
    this.wrapped = wrapped;
  }

  @Override
  public ServiceReference<S> getReference() {
    return wrapped.getReference();
  }

  @Override
  public void setProperties(final Dictionary<String, ?> properties) {
    wrapped.setProperties(properties);
  }

  @Override
  public void unregister() {
    this.component.removeServiceRegistration(this);
    wrapped.unregister();
  }

}
