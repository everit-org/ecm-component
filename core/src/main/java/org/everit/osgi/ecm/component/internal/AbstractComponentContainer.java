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

import org.everit.osgi.ecm.component.factory.ComponentContainerInstance;
import org.everit.osgi.ecm.component.internal.metatype.MetatypeProviderImpl;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

public abstract class AbstractComponentContainer<C> implements MetaTypeProvider, ComponentContainerInstance<C> {

    private final MetatypeProviderImpl<C> metatypeProvider;

    private BundleContext bundleContext;

    private ComponentMetadata componentMetadata;

    public AbstractComponentContainer(ComponentMetadata componentMetadata, BundleContext bundleContext) {
        this.componentMetadata = componentMetadata;
        this.bundleContext = bundleContext;
        if (componentMetadata.isMetatype()) {
            this.metatypeProvider = new MetatypeProviderImpl<C>(componentMetadata, bundleContext);
        } else {
            this.metatypeProvider = null;
        }
    }

    @Override
    public ComponentMetadata getComponentMetadata() {
        return componentMetadata;
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public String[] getLocales() {
        if (metatypeProvider == null) {
            return null;
        }
        return metatypeProvider.getLocales();
    }

    @Override
    public ObjectClassDefinition getObjectClassDefinition(String id, String locale) {
        if (metatypeProvider == null) {
            return null;
        }
        return metatypeProvider.getObjectClassDefinition(id, locale);
    }

}
