package org.everit.osgi.ecm.component.ri.internal.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.everit.osgi.ecm.component.resource.ServiceCapability;
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
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Map<String, String> getDirectives() {
        return Collections.emptyMap();
    }

    @Override
    public String getNamespace() {
        return "osgi.service";
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public ServiceReference<?> getServiceReference() {
        return serviceReference;
    }

}
