/**
 * This file is part of Everit - ECM Component RI Tests.
 *
 * Everit - ECM Component RI Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - ECM Component RI Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - ECM Component RI Tests.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.ecm.component.tests;

import org.everit.osgi.ecm.annotation.metadatabuilder.MetadataBuilder;
import org.everit.osgi.ecm.component.ri.ComponentContainerFactory;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ECMTestActivator implements BundleActivator {

    private ComponentContainerInstance<Object> bundleCapabilityComponent;
    private ComponentContainerInstance<ECMTest> ecmTestComponent;
    private ComponentContainerInstance<FactoryComponent> factoryComponent;
    private ComponentContainerInstance<IgnoredComponent> ignoredComponent;
    private ComponentContainerInstance<Object> testComponent;

    @Override
    public void start(final BundleContext context) throws Exception {
        ComponentContainerFactory factory = new ComponentContainerFactory(context);

        // ComponentMetadata factoryComponentMetadata = MetadataBuilder.buildComponentMetadata(FactoryComponent.class);
        //
        // factoryComponent = factory.createComponentContainer(factoryComponentMetadata);
        // factoryComponent.open();
        //
        // ComponentMetadata ignoredComponentMetadata = MetadataBuilder.buildComponentMetadata(IgnoredComponent.class);
        //
        // ignoredComponent = factory.createComponentContainer(ignoredComponentMetadata);
        // ignoredComponent.open();
        //
        // ComponentMetadata testComponentMetadata = MetadataBuilder.buildComponentMetadata(TestComponent.class);
        // testComponent = factory.createComponentContainer(testComponentMetadata);
        // testComponent.open();

        ComponentMetadata bundleCapabilityTest = MetadataBuilder
                .buildComponentMetadata(BundleCapabilityTestComponent.class);
        bundleCapabilityComponent = factory.createComponentContainer(bundleCapabilityTest);
        bundleCapabilityComponent.open();

        // ComponentMetadata ecmTest = MetadataBuilder.buildComponentMetadata(ECMTest.class);
        // ecmTestComponent = factory.createComponentContainer(ecmTest);
        // ecmTestComponent.open();
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        // ecmTestComponent.close();
        // testComponent.close();
        // factoryComponent.close();
        // ignoredComponent.close();
        bundleCapabilityComponent.close();
    }

}
