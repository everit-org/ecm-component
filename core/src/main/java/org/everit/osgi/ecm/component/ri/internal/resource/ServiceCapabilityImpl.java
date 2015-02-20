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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.everit.osgi.linkage.ServiceCapability;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;

public class ServiceCapabilityImpl implements ServiceCapability {

    private final Map<String, Object> attributes;

    private final BundleRevision resource;

    private final ServiceReference<?> serviceReference;

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

    @Override
    public Resource getResource() {
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
        result = prime * result + ((resource == null) ? 0 : resource.hashCode());
        result = prime * result + ((serviceReference == null) ? 0 : serviceReference.hashCode());
        return result;
    }

}
