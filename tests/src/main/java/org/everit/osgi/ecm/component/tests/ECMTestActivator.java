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

import java.util.Dictionary;
import java.util.Hashtable;

import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.ecm.annotation.metadatabuilder.MetadataBuilder;
import org.everit.osgi.ecm.component.factory.ComponentContainerFactory;
import org.everit.osgi.ecm.component.factory.ComponentContainerInstance;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ECMTestActivator implements BundleActivator {

    private ComponentContainerInstance<FactoryComponent> factoryComponent;
    private ComponentContainerInstance<IgnoredComponent> ignoredComponent;
    private ComponentContainerInstance<Object> testComponent;
    private ServiceRegistration<ECMTest> testServiceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        ComponentContainerFactory factory = new ComponentContainerFactory(context);

        ComponentMetadata factoryComponentMetadata = MetadataBuilder.buildComponentMetadata(FactoryComponent.class);

        factoryComponent = factory.createComponentContainer(factoryComponentMetadata);
        factoryComponent.open();

        ComponentMetadata ignoredComponentMetadata = MetadataBuilder.buildComponentMetadata(IgnoredComponent.class);

        ignoredComponent = factory.createComponentContainer(ignoredComponentMetadata);
        ignoredComponent.open();

        ComponentMetadata testComponentMetadata = MetadataBuilder.buildComponentMetadata(TestComponent.class);
        testComponent = factory.createComponentContainer(testComponentMetadata);
        testComponent.open();

        ECMTest ecmTest = new ECMTest();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(TestRunnerConstants.SERVICE_PROPERTY_TEST_ID, "ECMTest");
        properties.put(TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE, "junit4");
        testServiceRegistration = context.registerService(ECMTest.class, ecmTest, properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        testServiceRegistration.unregister();
        testComponent.close();
        factoryComponent.close();
        ignoredComponent.close();
    }

}
