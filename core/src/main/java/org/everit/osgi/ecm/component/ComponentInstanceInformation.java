package org.everit.osgi.ecm.component;

import java.util.Dictionary;
import java.util.Set;

import org.osgi.framework.ServiceReference;

public interface ComponentInstanceInformation {

    Dictionary<String, ?> getProperties();

    Set<ReferenceRuntime> getReferences();

    Set<ServiceReference<?>> getRegisteredServices();

    String getState();
}
