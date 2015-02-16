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
package org.everit.osgi.ecm.component.ri.internal;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.MetaTypeProvider;

public class FactoryComponentContainerImpl<C> extends AbstractComponentContainer<C> implements ManagedServiceFactory {

    private final Map<String, ComponentContextImpl<?>> components = new ConcurrentHashMap<String, ComponentContextImpl<?>>();

    private ServiceRegistration<?> serviceRegistration;

    public FactoryComponentContainerImpl(final ComponentMetadata componentMetadata, final BundleContext bundleContext) {
        super(componentMetadata, bundleContext);
    }

    @Override
    public void close() {
        Iterator<Entry<String, ComponentContextImpl<?>>> iterator = components.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<java.lang.String, ComponentContextImpl<?>> entry = iterator
                    .next();

            ComponentContextImpl<?> componentContextImpl = entry.getValue();
            componentContextImpl.close();
            iterator.remove();
        }

        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

    @Override
    public void deleted(final String pid) {
        ComponentContextImpl<?> component = components.get(pid);
        if (component != null) {
            component.close();
            components.remove(pid);
        }
    }

    @Override
    public ComponentRevision[] getComponentRevisions() {
        Collection<ComponentContextImpl<?>> values = components.values();

        ComponentRevision[] result = new ComponentRevision[values.size()];

        int i = 0;
        for (ComponentContextImpl<?> componentContextImpl : values) {
            result[i] = componentContextImpl.getComponentRevision();
            i++;
        }

        return result;
    }

    @Override
    public String getName() {
        return getComponentMetadata().getComponentId();
    }

    @Override
    public void open() {
        BundleContext context = getBundleContext();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        List<String> serviceInterfaces = new LinkedList<String>();
        serviceInterfaces.add(ComponentContainer.class.getName());

        ComponentMetadata componentMetadata = getComponentMetadata();
        if (componentMetadata.isMetatype()) {
            properties.put(MetaTypeProvider.METATYPE_FACTORY_PID, componentMetadata.getConfigurationPid());
            serviceInterfaces.add(MetaTypeProvider.class.getName());
        }
        addCommonServiceProperties(properties);

        properties.put(Constants.SERVICE_PID, componentMetadata.getConfigurationPid());
        serviceInterfaces.add(ManagedServiceFactory.class.getName());

        serviceRegistration = context.registerService(serviceInterfaces.toArray(new String[serviceInterfaces.size()]),
                this, properties);

    }

    @Override
    public void updated(final String pid, final Dictionary<String, ?> properties) throws ConfigurationException {
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = (Dictionary<String, Object>) properties;

        ComponentContextImpl<?> componentContextImpl = components.get(pid);
        if (componentContextImpl != null) {
            componentContextImpl.updateConfiguration(properties);
        } else {
            ComponentContextImpl<?> newComponent = new ComponentContextImpl<C>(getComponentMetadata(),
                    getBundleContext(), props);
            components.put(pid, newComponent);
            newComponent.open();
        }
    }
}
