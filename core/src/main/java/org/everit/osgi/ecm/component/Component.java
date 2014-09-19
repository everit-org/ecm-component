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
package org.everit.osgi.ecm.component;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;

import org.everit.osgi.ecm.component.internal.ManagedServiceFactoryImpl;
import org.everit.osgi.ecm.component.internal.ManagedServiceImpl;
import org.everit.osgi.ecm.component.internal.MetatypeProviderImpl;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.MetaTypeProvider;

public class Component<C> {

    private final BundleContext bundleContext;

    private final ComponentMetadata<C> componentMetadata;

    private Dictionary<String, Object> managedServiceProps;

    private ServiceRegistration<?> managedServiceSR;

    private final MetatypeProviderImpl<C> metatypeProviderImpl;

    private ServiceRegistration<MetaTypeProvider> metatypeProviderSR;

    public Component(ComponentMetadata<C> componentMetadata, BundleContext bundleContext) {
        this.componentMetadata = componentMetadata;
        this.bundleContext = bundleContext;

        metatypeProviderImpl = new MetatypeProviderImpl<C>(componentMetadata, bundleContext);

    }

    public void close() {
        if (managedServiceSR != null) {
            managedServiceSR.unregister();
        }
        if (metatypeProviderSR != null) {
            metatypeProviderSR.unregister();
        }
    }

    public void open() {
        String configurationPid = componentMetadata.getConfigurationPid();

        if (componentMetadata.isMetatype()) {
            registerMetatypeProvider(configurationPid);
        }

        managedServiceProps = new Hashtable<String, Object>();
        managedServiceProps.put(Constants.SERVICE_PID, configurationPid);
        if (componentMetadata.isConfigurationFactory()) {
            ManagedServiceFactory managedServiceFactory = new ManagedServiceFactoryImpl();
            managedServiceSR = bundleContext.registerService(ManagedServiceFactory.class, managedServiceFactory,
                    managedServiceProps);
        } else {
            ManagedService managedService = new ManagedServiceImpl();
            managedServiceSR = bundleContext.registerService(ManagedService.class, managedService, managedServiceProps);
        }
    }

    public void pushModifiedService() {
        Random r = new Random();

        Hashtable<String, Object> newProps = new Hashtable<String, Object>();
        Enumeration<String> keys = managedServiceProps.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            Object value = managedServiceProps.get(key);
            newProps.put(key, value);
        }
        newProps.put("someProp", r.nextInt());
        managedServiceSR.setProperties(newProps);
    }

    private void registerMetatypeProvider(String configurationPid) {
        boolean configurationFactory = componentMetadata.isConfigurationFactory();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        if (configurationFactory) {
            properties.put(MetaTypeProvider.METATYPE_FACTORY_PID, configurationPid);
        } else {
            properties.put(MetaTypeProvider.METATYPE_PID, configurationPid);
        }

        metatypeProviderSR = bundleContext
                .registerService(MetaTypeProvider.class, metatypeProviderImpl, properties);
    }
}
