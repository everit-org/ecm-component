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

import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.component.ri.ComponentContainerFactory;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata.ComponentMetadataBuilder;
import org.everit.osgi.ecm.metadata.ServiceMetadata;
import org.everit.osgi.ecm.metadata.ServiceMetadata.ServiceMetadataBuilder;
import org.everit.osgi.ecm.metadata.ServiceReferenceMetadata.ServiceReferenceMetadataBuilder;
import org.everit.osgi.ecm.metadata.StringAttributeMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;

/**
 * Testing circular wiring between components.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
@StringAttributes({
    @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE,
        defaultValue = "junit4"),
    @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TEST_ID,
        defaultValue = "CircularityTest") })
@Service
public class CircularityTest {

  /**
   * A class that does nothing but has a setter that makes it circular dependent.
   *
   */
  public static class CircularClass {
    public void setReference(final CircularClass reference) {
      // do nothing
    }
  }

  private BundleContext bundleContext;

  @Activate
  public void activate(final BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  private ComponentContainerInstance<Object> createCircularComponent(
      final ComponentContainerFactory factory, final String servicePropValue,
      final String referencedServicePropValue, final boolean referenceDynamic) {
    // Create first non-dependent component
    ComponentMetadataBuilder cmetadataBuilder = new ComponentMetadataBuilder();
    cmetadataBuilder.withType(CircularClass.class.getName())
        .withConfigurationPolicy(org.everit.osgi.ecm.metadata.ConfigurationPolicy.IGNORE);
    AttributeMetadata<String[]> servicePropAttr =
        new StringAttributeMetadata.StringAttributeMetadataBuilder().withAttributeId("circularId")
            .withDefaultValue(new String[] { servicePropValue }).build();

    if (referencedServicePropValue == null) {
      cmetadataBuilder.withAttributes(new AttributeMetadata[] { servicePropAttr });
    } else {
      AttributeMetadata<String[]> referenceMetadata =
          new ServiceReferenceMetadataBuilder().withReferenceId("reference")
              .withServiceInterface(CircularClass.class)
              .withDefaultValue(new String[] { "(circularId=" + referencedServicePropValue + ")" })
              .withSetter(new MethodDescriptor("setReference"))
              .withDynamic(referenceDynamic)
              .build();

      cmetadataBuilder
          .withAttributes(new AttributeMetadata[] { servicePropAttr, referenceMetadata });
    }

    ServiceMetadata serviceMetadata =
        new ServiceMetadataBuilder().withClazzes(new Class[] { CircularClass.class }).build();
    cmetadataBuilder.withService(serviceMetadata);

    ComponentMetadata componentMetadata = cmetadataBuilder.build();

    return factory.createComponentContainer(componentMetadata);
  }

  @Test
  @TestDuringDevelopment
  public void testDynamicSingleCircularity() {
    ComponentContainerFactory factory = new ComponentContainerFactory(bundleContext);
    ComponentContainerInstance<Object> nonDependentComponent =
        createCircularComponent(factory, "dynamic", null, false);

    nonDependentComponent.open();

    ComponentContainerInstance<Object> circularComponent =
        createCircularComponent(factory, "dynamic", "dynamic", true);

    circularComponent.open();

    try {
      Assert.assertEquals(ComponentState.ACTIVE, circularComponent.getResources()[0].getState());

      nonDependentComponent.close();
      nonDependentComponent = null;

      ComponentRevision<Object> componentRevision = circularComponent.getResources()[0];
      Assert.assertEquals(ComponentState.ACTIVE, componentRevision.getState());
    } finally {
      if (nonDependentComponent != null) {
        nonDependentComponent.close();
      }

      circularComponent.close();
    }
  }

  @Test
  public void testNonDynamicSingleCircularity() {
    ComponentContainerFactory factory = new ComponentContainerFactory(bundleContext);
    ComponentContainerInstance<Object> nonDependentComponent =
        createCircularComponent(factory, "nonDynamic", null, false);

    nonDependentComponent.open();

    ComponentContainerInstance<Object> circularComponent =
        createCircularComponent(factory, "nonDynamic", "nonDynamic", false);

    circularComponent.open();

    try {
      ComponentRevision<Object> componentRevision = circularComponent.getResources()[0];
      Assert.assertEquals(ComponentState.ACTIVE, componentRevision.getState());

      nonDependentComponent.close();
      nonDependentComponent = null;

      componentRevision = circularComponent.getResources()[0];
      Assert.assertEquals(ComponentState.UNSATISFIED, componentRevision.getState());

    } finally {
      if (nonDependentComponent != null) {
        nonDependentComponent.close();
      }

      circularComponent.close();
    }
  }
}
