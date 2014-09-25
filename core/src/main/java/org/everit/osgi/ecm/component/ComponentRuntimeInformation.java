package org.everit.osgi.ecm.component;

import org.everit.osgi.ecm.metadata.ComponentMetadata;

public interface ComponentRuntimeInformation<C> {

    ComponentInstanceInformation[] getComponentInstances();

    ComponentMetadata<C> getComponentMetadata();

}
