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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.everit.osgi.linkage.ServiceCapability;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;

/**
 * Implementation of {@link ServiceCapability}.
 */
public class ServiceCapabilityImpl implements ServiceCapability {

  private final Map<String, Object> attributes;

  private final BundleRevision resource;

  private final ServiceReference<?> serviceReference;

  /**
   * Constructor.
   *
   * @param serviceReference
   *          All information is derived from the {@link ServiceReference}.
   */
  public ServiceCapabilityImpl(final ServiceReference<?> serviceReference) {
    this.serviceReference = serviceReference;
    resource = serviceReference.getBundle().adapt(BundleRevision.class);
    HashMap<String, Object> serviceProps = new HashMap<String, Object>();
    String[] propertyKeys = serviceReference.getPropertyKeys();
    for (String propertyKey : propertyKeys) {
      serviceProps.put(propertyKey, serviceReference.getProperty(propertyKey));
    }
    attributes = Collections.unmodifiableMap(serviceProps);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ServiceCapabilityImpl other = (ServiceCapabilityImpl) obj;
    if (resource == null) {
      if (other.resource != null) {
        return false;
      }
    } else if (!resource.equals(other.resource)) {
      return false;
    }
    if (serviceReference == null) {
      if (other.serviceReference != null) {
        return false;
      }
    } else if (!serviceReference.equals(other.serviceReference)) {
      return false;
    }
    return true;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Map<String, String> getDirectives() {
    return Collections.emptyMap();
  }

  @Override
  public String getNamespace() {
    return ServiceCapability.SERVICE_CAPABILITY_NAMESPACE;
  }

  /**
   * Always the {@link BundleRevision} that currently belongs to the bundle whose context was used
   * to register the OSGi service.
   * */
  @Override
  public BundleRevision getResource() {
    return resource;
  }

  @Override
  public ServiceReference<?> getServiceReference() {
    return serviceReference;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + ((resource == null) ? 0 : resource.hashCode());
    result = (prime * result) + ((serviceReference == null) ? 0 : serviceReference.hashCode());
    return result;
  }

}
