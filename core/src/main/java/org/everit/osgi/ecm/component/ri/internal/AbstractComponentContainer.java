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

import java.util.Dictionary;

import org.everit.osgi.ecm.component.ECMComponentConstants;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.component.ri.internal.metatype.MetatypeProviderImpl;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

public abstract class AbstractComponentContainer<C> implements MetaTypeProvider, ComponentContainerInstance<C> {

    private final BundleContext bundleContext;

    private final ComponentMetadata componentMetadata;

    private final MetatypeProviderImpl<C> metatypeProvider;

    public AbstractComponentContainer(final ComponentMetadata componentMetadata, final BundleContext bundleContext) {
        this.componentMetadata = componentMetadata;
        this.bundleContext = bundleContext;
        this.metatypeProvider = new MetatypeProviderImpl<C>(componentMetadata, bundleContext);
    }

    protected void addCommonServiceProperties(final Dictionary<String, Object> properties) {
        properties.put(ECMComponentConstants.SERVICE_PROP_COMPONENT_CLASS, componentMetadata.getType());
        properties.put(ECMComponentConstants.SERVICE_PROP_COMPONENT_NAME, this.metatypeProvider
                .getObjectClassDefinition(null, null).getName());
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public ComponentMetadata getComponentMetadata() {
        return componentMetadata;
    }

    @Override
    public String[] getLocales() {
        return metatypeProvider.getLocales();
    }

    @Override
    public ObjectClassDefinition getObjectClassDefinition(final String id, final String locale) {
        return metatypeProvider.getObjectClassDefinition(id, locale);
    }

}
