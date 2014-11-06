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
package org.everit.osgi.ecm.component.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.ConfigurationPolicy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.metatype.MetaTypeProvider;

public class ComponentContainerImpl<C> extends AbstractComponentContainer<C> implements ManagedService {

    private final AtomicReference<ComponentContextImpl<C>> componentAtomicReference = new AtomicReference<ComponentContextImpl<C>>();

    private ServiceRegistration<?> serviceRegistration;

    public ComponentContainerImpl(ComponentMetadata componentMetadata, BundleContext bundleContext) {
        super(componentMetadata, bundleContext);
    }

    @Override
    public void close() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
        ComponentContextImpl<C> componentImpl = componentAtomicReference.get();
        if (componentImpl != null) {
            componentImpl.close();
            componentAtomicReference.set(null);
        }
    }

    @Override
    public ComponentRevision[] getComponentRevisions() {
        ComponentContextImpl<C> componentImpl = componentAtomicReference.get();
        if (componentImpl == null) {
            return new ComponentRevision[0];
        }
        return new ComponentRevision[] { componentImpl.getComponentRevision() };
    }

    @Override
    public void open() {
        BundleContext context = getBundleContext();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        List<String> serviceInterfaces = new LinkedList<String>();
        serviceInterfaces.add(ComponentContainer.class.getName());

        ComponentMetadata componentMetadata = getComponentMetadata();

        if (!ConfigurationPolicy.IGNORE.equals(componentMetadata.getConfigurationPolicy())) {
            if (componentMetadata.isMetatype()) {
                properties.put(MetaTypeProvider.METATYPE_PID, componentMetadata.getConfigurationPid());
                serviceInterfaces.add(MetaTypeProvider.class.getName());
            }
            properties.put(Constants.SERVICE_PID, componentMetadata.getConfigurationPid());
            serviceInterfaces.add(ManagedService.class.getName());
        }

        serviceRegistration = context.registerService(serviceInterfaces.toArray(new String[serviceInterfaces.size()]),
                this, properties);

        ComponentContextImpl<C> componentImpl = componentAtomicReference.get();
        if (ConfigurationPolicy.IGNORE.equals(componentMetadata.getConfigurationPolicy()) && componentImpl == null) {
            componentImpl = new ComponentContextImpl<C>(componentMetadata, getBundleContext());
            componentAtomicReference.set(componentImpl);
            componentImpl.open();
            return;
        }

    }

    @Override
    public synchronized void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = (Dictionary<String, Object>) properties;

        ComponentMetadata componentMetadata = getComponentMetadata();
        ComponentContextImpl<C> componentImpl = componentAtomicReference.get();

        ConfigurationPolicy configurationPolicy = componentMetadata.getConfigurationPolicy();

        if (componentImpl == null && (properties != null || ConfigurationPolicy.OPTIONAL.equals(configurationPolicy))) {
            componentImpl = new ComponentContextImpl<C>(componentMetadata, getBundleContext(), props);
            componentAtomicReference.set(componentImpl);
            componentImpl.open();
        } else if (componentImpl != null && properties == null
                && !ConfigurationPolicy.OPTIONAL.equals(configurationPolicy)) {

            componentImpl.close();
            componentAtomicReference.set(null);

        } else if (componentImpl != null) {
            @SuppressWarnings("unchecked")
            Dictionary<String, Object> propertiesWithObjectGenerics = (Dictionary<String, Object>) properties;
            componentImpl.updateConfiguration(propertiesWithObjectGenerics);
        }
    }
}
