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
package org.everit.osgi.ecm.component.ri;

import org.everit.osgi.ecm.component.ri.internal.ComponentContainerImpl;
import org.everit.osgi.ecm.component.ri.internal.FactoryComponentContainerImpl;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.ConfigurationPolicy;
import org.osgi.framework.BundleContext;

public class ComponentContainerFactory {

    private final BundleContext bundleContext;

    public ComponentContainerFactory(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public <C> ComponentContainerInstance<C> createComponentContainer(ComponentMetadata componentMetadata) {
        if (ConfigurationPolicy.FACTORY.equals(componentMetadata.getConfigurationPolicy())) {
            return new FactoryComponentContainerImpl<C>(componentMetadata, bundleContext);
        } else {
            return new ComponentContainerImpl<C>(componentMetadata, bundleContext);
        }
    }
}
