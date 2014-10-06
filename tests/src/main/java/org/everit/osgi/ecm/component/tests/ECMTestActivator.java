/**
 * This file is part of Everit - ECM Component Tests.
 *
 * Everit - ECM Component Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - ECM Component Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - ECM Component Tests.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.ecm.component.tests;

import org.everit.osgi.ecm.annotation.metadatabuilder.MetadataBuilder;
import org.everit.osgi.ecm.component.ComponentContainerFactory;
import org.everit.osgi.ecm.component.ComponentContainerInstance;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ECMTestActivator implements BundleActivator {

    private ComponentContainerInstance<AnnotatedClass> component;
    private ComponentContainerInstance<IgnoredComponent> otherComponent;

    @Override
    public void start(BundleContext context) throws Exception {
        ComponentMetadata componentMetadata = MetadataBuilder
                .buildComponentMetadata(AnnotatedClass.class);

        ComponentContainerFactory factory = new ComponentContainerFactory(context);

        component = factory.createComponentContainer(componentMetadata);
        component.open();

        ComponentMetadata otherComponentMetadata = MetadataBuilder
                .buildComponentMetadata(IgnoredComponent.class);

        otherComponent = factory.createComponentContainer(otherComponentMetadata);
        otherComponent.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        component.close();
        otherComponent.close();
    }

}
