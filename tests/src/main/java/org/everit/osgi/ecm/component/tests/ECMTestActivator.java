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
import org.everit.osgi.ecm.component.Component;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ECMTestActivator implements BundleActivator {

    private Component<AnnotatedClass> component;
    private Component<OtherAnnotatedClass> otherComponent;

    @Override
    public void start(BundleContext context) throws Exception {
        ComponentMetadata<AnnotatedClass> componentMetadata = MetadataBuilder
                .buildComponentMetadata(AnnotatedClass.class);

        component = new Component<AnnotatedClass>(componentMetadata, context);
        component.open();

        component.pushModifiedService();
        component.pushModifiedService();
        ComponentMetadata<OtherAnnotatedClass> otherComponentMetadata = MetadataBuilder
                .buildComponentMetadata(OtherAnnotatedClass.class);

        otherComponent = new Component<OtherAnnotatedClass>(otherComponentMetadata, context);
        otherComponent.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        component.close();
        otherComponent.close();
    }

}
