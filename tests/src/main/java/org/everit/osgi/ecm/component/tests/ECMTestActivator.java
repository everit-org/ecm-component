/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.ecm.component.tests;

import org.everit.osgi.ecm.annotation.metadatabuilder.MetadataBuilder;
import org.everit.osgi.ecm.component.ri.ComponentContainerFactory;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator that starts the test components.
 *
 */
public class ECMTestActivator implements BundleActivator {

  private ComponentContainerInstance<ECMTest> ecmTestComponent;

  private ComponentContainerInstance<FactoryComponent> factoryComponent;

  @Override
  public void start(final BundleContext context) throws Exception {
    ComponentContainerFactory factory = new ComponentContainerFactory(context);

    ComponentMetadata factoryComponentMetadata = MetadataBuilder
        .buildComponentMetadata(FactoryComponent.class);

    factoryComponent = factory.createComponentContainer(factoryComponentMetadata);
    factoryComponent.open();

    ComponentMetadata ecmTest = MetadataBuilder.buildComponentMetadata(ECMTest.class);
    ecmTestComponent = factory.createComponentContainer(ecmTest);
    ecmTestComponent.open();
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    ecmTestComponent.close();
    factoryComponent.close();
  }

}
