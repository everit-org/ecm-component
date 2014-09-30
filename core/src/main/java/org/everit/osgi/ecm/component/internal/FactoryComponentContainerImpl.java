/**
 * This file is part of Everit - ECM Component.
 *
 * Everit - ECM Component is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - ECM Component is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - ECM Component.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.ecm.component.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

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

    private ServiceRegistration<?> serviceRegistration;

    public FactoryComponentContainerImpl(ComponentMetadata<C> componentMetadata, BundleContext bundleContext) {
        super(componentMetadata, bundleContext);
    }

    @Override
    public void close() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

    @Override
    public void deleted(String pid) {
        System.out.println("ManagedServiceFactory deleted called with pid: " + pid);
        // TODO Auto-generated method stub

    }

    @Override
    public ComponentRevision[] getComponentRevisions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        System.out.println("ManagedServiceFactory getName called");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void open() {
        BundleContext context = getBundleContext();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        List<String> serviceInterfaces = new LinkedList<String>();
        serviceInterfaces.add(ComponentContainer.class.getName());

        ComponentMetadata<C> componentMetadata = getComponentMetadata();
        if (componentMetadata.isMetatype()) {
            properties.put(MetaTypeProvider.METATYPE_FACTORY_PID, componentMetadata.getConfigurationPid());
            serviceInterfaces.add(MetaTypeProvider.class.getName());
        }

        properties.put(Constants.SERVICE_PID, componentMetadata.getConfigurationPid());
        serviceInterfaces.add(ManagedServiceFactory.class.getName());

        serviceRegistration = context.registerService(serviceInterfaces.toArray(new String[serviceInterfaces.size()]),
                this, properties);

    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        System.out
                .println("ManagedServiceFactory updated called: pid=" + pid + ", properties=" + properties.toString());
        // TODO Auto-generated method stub

    }
}
