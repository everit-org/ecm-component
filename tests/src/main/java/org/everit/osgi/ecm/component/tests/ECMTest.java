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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.annotation.metadatabuilder.MetadataBuilder;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.ECMComponentConstants;
import org.everit.osgi.ecm.component.PasswordHolder;
import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.component.ri.ComponentContainerFactory;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.resource.Capability;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The component that contains the test functions.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
@StringAttributes({
    @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE,
        defaultValue = "junit4"),
    @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TEST_ID,
        defaultValue = "ECMTest") })
@Service
@TestDuringDevelopment
public class ECMTest {

  private static final double TEST_VALUE_DOUBLE = 1.1D;

  private static final float TEST_VALUE_FLOAT = 1.1F;

  private static final int WAITING_TIMEOUT = 1000;

  private ComponentContext<ECMTest> componentContext;

  private ConfigurationAdmin configAdmin;

  private Set<Configuration> configurations = new HashSet<>();

  private ComponentContainerFactory factory;

  @Activate
  public void activate(final ComponentContext<ECMTest> componentContext) {
    this.componentContext = componentContext;
    factory = new ComponentContainerFactory(componentContext.getBundleContext());
  }

  /**
   * Freeing up all configurations that were created within configurationAdmin while running the
   * test.
   */
  @After
  public void after() {
    for (Configuration configuration : configurations) {
      try {
        configuration.delete();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    configurations.clear();
  }

  private Hashtable<String, Object> createPresetPropertiesForEveryTypeAttributeTestComponent() {
    Hashtable<String, Object> properties = new Hashtable<String, Object>();
    properties.put("booleanAttribute", true);
    properties.put("booleanArrayAttribute", new boolean[] { true });

    // Testing if one size array is passed to a non-multiple attribute
    properties.put("byteAttribute", new byte[] { 1 });
    properties.put("byteArrayAttribute", new byte[] { 1 });

    properties.put("charAttribute", 'a');
    properties.put("charArrayAttribute", new char[] { 'a' });

    properties.put("doubleAttribute", TEST_VALUE_DOUBLE);
    properties.put("doubleArrayAttribute", new double[] { TEST_VALUE_DOUBLE });

    properties.put("floatAttribute", TEST_VALUE_FLOAT);
    properties.put("floatArrayAttribute", new float[] { TEST_VALUE_FLOAT });

    properties.put("intAttribute", 1);
    properties.put("intArrayAttribute", new int[] { 1 });

    properties.put("longAttribute", 1L);
    properties.put("longArrayAttribute", new long[] { 1L });

    properties.put("shortAttribute", (short) 1);
    properties.put("shortArrayAttribute", new short[] { 1 });

    properties.put("passwordAttribute", "123456");
    properties.put("passwordArrayAttribute", new String[] { "123456" });

    properties.put("stringAttribute", "Hello World");
    properties.put("stringArrayAttribute", new String[] { "Hello World" });
    return properties;
  }

  private ComponentState getComponentState(
      final ComponentContainerInstance<?> container) {
    ComponentRevision<?>[] resources = container.getResources();
    if (resources.length == 0) {
      return null;
    }
    return resources[0].getState();
  }

  @ServiceRef(defaultValue = "(service.id>=0)")
  public void setConfigAdmin(final ConfigurationAdmin configAdmin) {
    this.configAdmin = configAdmin;
  }

  @Test
  public void testBundleCapabilityTestComponent() {
    ComponentMetadata bundleCapabilityTest = MetadataBuilder
        .buildComponentMetadata(BundleCapabilityTestComponent.class);
    ComponentContainerInstance<BundleCapabilityTestComponent> container = factory
        .createComponentContainer(bundleCapabilityTest);
    container.open();

    Configuration configuration = null;
    try {

      configuration = configAdmin.getConfiguration(BundleCapabilityTestComponent.class.getName(),
          null);

      Hashtable<String, Object> properties = new Hashtable<String, Object>();

      properties.put("bcArrayReference.target", new String[] {});
      properties.put("bcHolderReference.target", new String[] {});
      properties.put("bcReference.target", new String[] {});

      configuration.update(properties);

      waitForService(BundleCapabilityTestComponent.class);

      properties.put("bcArrayReference.target", new String[] { "(testAttribute=1)" });
      configuration.update(properties);

      waitForService(BundleCapabilityTestComponent.class);

    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (configuration != null) {
          configuration.delete();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      container.close();
    }

  }

  @Test
  public void testComponentUnregistersServiceAfterGettingUnsatisfied() {
    ComponentMetadata testComponentMetadata = MetadataBuilder
        .buildComponentMetadata(EveryTypeAttributeTestComponent.class);
    ComponentContainerInstance<Object> container = factory
        .createComponentContainer(testComponentMetadata);
    container.open();

    Hashtable<String, Object> properties = createPresetPropertiesForEveryTypeAttributeTestComponent();
    properties.put("testComponentUnregistersServiceAfterGettingUnsatisfied", true);
    updateConfiguration(container, properties);

    try {
      String filterString = "(testComponentUnregistersServiceAfterGettingUnsatisfied=true)";
      waitForService(filterString);
      properties.put("someReference.target", "(nonExistent=nonExistent)");
      updateConfiguration(container, properties);
      waitForTrueSupplied(
          () -> container.getResources()[0].getState() == ComponentState.UNSATISFIED);

      BundleContext bundleContext = componentContext.getBundleContext();

      Collection<ServiceReference<EveryTypeAttributeTestComponent>> serviceReferences =
          bundleContext.getServiceReferences(EveryTypeAttributeTestComponent.class, filterString);

      Assert.assertEquals(0, serviceReferences.size());
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    } finally {
      container.close();
    }
  }

  @Test
  public void testCustomServicePropertiesOnComponentRegisteredService() {

    Configuration configuration = null;
    try {

      configuration = configAdmin.getConfiguration(SimpleComponent.COMPONENT_ID, null);
      Dictionary<String, Object> properties = new Hashtable<>();
      properties.put("simpleStringAttr", "simple string value");
      configuration.update(properties);

      waitForService(SimpleComponent.class);

      ServiceReference<SimpleComponent> simpleComponentReference = componentContext
          .getBundleContext().getServiceReference(SimpleComponent.class);

      Object componentId = simpleComponentReference
          .getProperty(ECMComponentConstants.SERVICE_PROP_COMPONENT_ID);

      Collection<ServiceReference<ManagedService>> simpleComponentContainerReferences =
          componentContext.getBundleContext().getServiceReferences(
              ManagedService.class,
              "(" + ECMComponentConstants.SERVICE_PROP_COMPONENT_ID
                  + "=" + componentId
                  + ")");

      Assert.assertEquals(1, simpleComponentContainerReferences.size());

      Assert.assertEquals(SimpleComponent.COMPONENT_ID, componentId);

    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (configuration != null) {
        try {
          configuration.delete();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  @Test
  public void testFailingComponent() {

    ComponentMetadata componentMetadata = MetadataBuilder
        .buildComponentMetadata(FailingComponent.class);

    ComponentContainerInstance<FailingComponent> container = factory
        .createComponentContainer(componentMetadata);

    container.open();

    try {
      ComponentRevision<FailingComponent>[] resources = container.getResources();
      Assert.assertEquals(0, resources.length);

      testFailingWithEmptyProperties(container);
      testFailingWithReferenceProbe(container);
      testFailingWithSetterProbe(container);
      testFailingWithDynamicSetterProbe(container);
      testFailingWithUpdateProbe(container);
    } finally {
      container.close();
    }
  }

  private void testFailingWithDynamicSetterProbe(
      final ComponentContainerInstance<FailingComponent> container) {

    Hashtable<String, Object> configuration = new Hashtable<>();
    configuration.put(FailingComponent.FAIL_DYNAMIC_PROPERTY_SETTER_ATTRIBUTE, true);
    updateConfiguration(container, configuration);
    waitForTrueSupplied(() -> getComponentState(container) == ComponentState.FAILED);

    Assert.assertEquals(ComponentState.FAILED, getComponentState(container));

    configuration.remove(FailingComponent.FAIL_DYNAMIC_PROPERTY_SETTER_ATTRIBUTE);
    updateConfiguration(container, configuration);
    waitForTrueSupplied(() -> getComponentState(container) == ComponentState.ACTIVE);

    Assert.assertEquals(ComponentState.ACTIVE, getComponentState(container));
  }

  private void testFailingWithEmptyProperties(
      final ComponentContainerInstance<FailingComponent> container) {
    Hashtable<String, Object> properties = new Hashtable<String, Object>();
    updateConfiguration(container, properties);
    waitForTrueSupplied(() -> getComponentState(container) == ComponentState.ACTIVE);
    Assert.assertEquals(1, container.getResources().length);
  }

  private void testFailingWithReferenceProbe(
      final ComponentContainerInstance<FailingComponent> container) {

    ServiceRegistration<String> serviceRegistration = null;
    ServiceRegistration<String> failingServiceRegistration = null;

    try {
      Hashtable<String, Object> configuration = new Hashtable<>();
      configuration.put("failingReference.target", "(name=forFailing)");

      updateConfiguration(container, configuration);

      Hashtable<String, Object> properties = new Hashtable<>();
      properties.put("name", "forFailing");
      serviceRegistration = componentContext.registerService(
          String.class, "", properties);

      waitForService("(failingReference.target=\\(name\\=forFailing\\))");
      waitForTrueSupplied(() -> ComponentState.ACTIVE == getComponentState(container));
      Assert.assertEquals(ComponentState.ACTIVE, getComponentState(container));
      serviceRegistration.unregister();
      serviceRegistration = null;

      Assert.assertEquals(ComponentState.UNSATISFIED, getComponentState(container));

      failingServiceRegistration = componentContext.registerService(
          String.class, FailingComponent.CONF_FAIL_REFERENCE, properties);

      Assert.assertEquals(ComponentState.FAILED, getComponentState(container));

      serviceRegistration = componentContext.registerService(
          String.class, "", properties);

      Assert.assertEquals(ComponentState.FAILED, getComponentState(container));

      failingServiceRegistration.unregister();
      failingServiceRegistration = null;

      Assert.assertEquals(ComponentState.ACTIVE, getComponentState(container));

      updateConfiguration(container, configuration);
      waitForTrueSupplied(() -> ComponentState.ACTIVE == getComponentState(container));
      Assert.assertEquals(ComponentState.ACTIVE, getComponentState(container));
    } finally {
      if (failingServiceRegistration != null) {
        failingServiceRegistration.unregister();
      }

      if (serviceRegistration != null) {
        serviceRegistration.unregister();
      }
    }
  }

  private void testFailingWithSetterProbe(
      final ComponentContainerInstance<FailingComponent> container) {

    Hashtable<String, Object> configuration = new Hashtable<>();
    configuration.put(FailingComponent.FAIL_PROPERTY_SETTER_ATTRIBUTE, true);
    updateConfiguration(container, configuration);

    waitForTrueSupplied(() -> ComponentState.FAILED == getComponentState(container));
    Assert.assertEquals(ComponentState.FAILED, getComponentState(container));

    configuration.put(FailingComponent.FAIL_PROPERTY_SETTER_ATTRIBUTE, false);
    updateConfiguration(container, configuration);

    waitForTrueSupplied(() -> ComponentState.ACTIVE == getComponentState(container));
    Assert.assertEquals(ComponentState.ACTIVE, getComponentState(container));

  }

  private void testFailingWithUpdateProbe(
      final ComponentContainerInstance<FailingComponent> container) {

    Hashtable<String, Object> configuration = new Hashtable<>();
    configuration.put(FailingComponent.FAIL_ON_UPDATE_ATTRIBUTE, false);
    updateConfiguration(container, configuration);

    configuration.put(FailingComponent.FAIL_ON_UPDATE_ATTRIBUTE, true);
    updateConfiguration(container, configuration);

    waitForTrueSupplied(() -> ComponentState.FAILED == getComponentState(container));
    Assert.assertEquals(ComponentState.FAILED, getComponentState(container));

    configuration.put(FailingComponent.FAIL_ON_UPDATE_ATTRIBUTE, false);
    updateConfiguration(container, configuration);

    waitForTrueSupplied(() -> ComponentState.ACTIVE == getComponentState(container));
    Assert.assertEquals(ComponentState.ACTIVE, getComponentState(container));
  }

  @Test
  public void testIgnoredComponent() {
    ComponentMetadata ignoredComponentMetadata = MetadataBuilder
        .buildComponentMetadata(IgnoredComponent.class);

    ComponentContainerInstance<IgnoredComponent> ignoredComponentContainer = factory
        .createComponentContainer(ignoredComponentMetadata);
    ignoredComponentContainer.open();

    try {
      IgnoredComponent ignoredComponent = waitForService(IgnoredComponent.class);

      Assert.assertEquals("Default", ignoredComponent.getPropertyWithDefaultValue());
      Assert.assertEquals(null, ignoredComponent.getPropertyWithoutDefaultValue());
    } finally {
      ignoredComponentContainer.close();
    }
  }

  @Test
  public void testMetatypeWithCapabilitiesAndRequirements() {
    ComponentMetadata componentMetadata = MetadataBuilder
        .buildComponentMetadata(MultiRequirementAndCapabilityComponent.class);

    ComponentContainerInstance<Object> container = factory
        .createComponentContainer(componentMetadata);

    container.open();
    try {
      waitForService(MultiRequirementAndCapabilityComponent.class);
      ComponentRevision<Object> componentRevision = container.getResources()[0];
      List<Capability> capabilities = componentRevision.getCapabilities(null);
      int i = 0;
      final int maxIterationNum = 10;
      final long millisToSleep = 10;
      while (capabilities.size() < 2 && i < maxIterationNum) {
        Thread.sleep(millisToSleep);
        componentRevision = container.getResources()[0];
        capabilities = componentRevision.getCapabilities(null);
        i++;
      }

      Assert.assertEquals(2, capabilities.size());
      final int expectedRequirementNum = 3;
      Assert.assertEquals(expectedRequirementNum, componentRevision.getRequirements(null).size());

      MetaTypeProvider metatypeProvider = (MetaTypeProvider) container;

      ObjectClassDefinition objectClassDefinition = metatypeProvider.getObjectClassDefinition(
          MultiRequirementAndCapabilityComponent.class.getName(), null);

      AttributeDefinition[] attributeDefinitions = objectClassDefinition
          .getAttributeDefinitions(ObjectClassDefinition.ALL);

      Assert.assertEquals(1, attributeDefinitions.length);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } finally {
      container.close();
    }
  }

  @Test
  public void testPasswordAttribute() {
    ComponentMetadata componentMetadata = MetadataBuilder
        .buildComponentMetadata(PasswordHolderTestComponent.class);

    ComponentContainerInstance<PasswordHolderTestComponent> container = factory
        .createComponentContainer(componentMetadata);

    container.open();

    try {
      PasswordHolderTestComponent service = waitForService(PasswordHolderTestComponent.class);
      Assert.assertArrayEquals(
          new PasswordHolder[] { new PasswordHolder("test1"), new PasswordHolder("test2") },
          service.getNoSetterMultiPassword());

      Assert.assertArrayEquals(
          new PasswordHolder[] { new PasswordHolder("test1"), new PasswordHolder("test2") },
          service.getPshArrayPassword());

      Assert.assertArrayEquals(new String[] { "test1", "test2" }, service.getStringArrayPassword());

      Assert.assertEquals("test", service.getNoSetterPassword().getPassword());
      Assert.assertEquals("test", service.getPshPassword().getPassword());
      Assert.assertEquals("test", service.getSimpleStringPassword());

      Filter filter;
      try {
        filter = FrameworkUtil.createFilter("(noSetterMultiPassword=test1)");
      } catch (InvalidSyntaxException e) {
        throw new RuntimeException(e);
      }

      ServiceTracker<PasswordHolderTestComponent, PasswordHolderTestComponent> tracker =
          new ServiceTracker<>(componentContext.getBundleContext(), filter, null);

      tracker.open();

      try {
        Assert.assertEquals(1, tracker.getServices().length);
      } finally {
        tracker.close();
      }

    } finally {
      container.close();
    }
  }

  @Test
  public void testTestComponent() {
    ComponentMetadata testComponentMetadata = MetadataBuilder
        .buildComponentMetadata(EveryTypeAttributeTestComponent.class);
    ComponentContainerInstance<Object> container = factory
        .createComponentContainer(testComponentMetadata);
    container.open();

    Hashtable<String, Object> properties = createPresetPropertiesForEveryTypeAttributeTestComponent();

    updateConfiguration(container, properties);

    try {
      EveryTypeAttributeTestComponent testComponent = waitForService(EveryTypeAttributeTestComponent.class);

      // Check if all of the properties got the right value
      Assert.assertTrue(testComponent.getBooleanAttribute());
      Assert.assertTrue(Arrays.equals(new boolean[] { true },
          testComponent.getBooleanArrayAttribute()));

      Assert.assertEquals((byte) 1, testComponent.getByteAttribute());
      Assert.assertArrayEquals(new byte[] { 1 }, testComponent.getByteArrayAttribute());

      Assert.assertEquals('a', testComponent.getCharAttribute());
      Assert.assertArrayEquals(new char[] { 'a' }, testComponent.getCharArrayAttribute());

      Assert.assertEquals(TEST_VALUE_DOUBLE, testComponent.getDoubleAttribute(), 0);
      Assert.assertTrue(Arrays.equals(new double[] { TEST_VALUE_DOUBLE },
          testComponent.getDoubleArrayAttribute()));

      Assert.assertEquals(TEST_VALUE_FLOAT, testComponent.getFloatAttribute(), 0);
      Assert.assertTrue(Arrays.equals(new float[] { TEST_VALUE_FLOAT },
          testComponent.getFloatArrayAttribute()));

      Assert.assertTrue(1 == testComponent.getIntAttribute());
      Assert.assertTrue(Arrays.equals(new int[] { 1 }, testComponent.getIntArrayAttribute()));

      Assert.assertTrue(1L == testComponent.getLongAttribute());
      Assert.assertTrue(Arrays.equals(new long[] { 1L }, testComponent.getLongArrayAttribute()));

      Assert.assertTrue(1 == testComponent.getShortAttribute());
      Assert.assertTrue(Arrays.equals(new short[] { 1 }, testComponent.getShortArrayAttribute()));

      Assert.assertEquals("123456", testComponent.getPasswordAttribute());
      Assert.assertArrayEquals(new String[] { "123456" },
          testComponent.getPasswordArrayAttribute());

      Assert.assertEquals("Hello World", testComponent.getStringAttribute());
      Assert.assertArrayEquals(new String[] { "Hello World" },
          testComponent.getStringArrayAttribute());

    } finally {
      container.close();
    }
  }

  @Test
  public void testWrongActivateMethodComponent() {
    BundleContext bundleContext = componentContext.getBundleContext();
    ComponentContainerFactory factory = new ComponentContainerFactory(bundleContext);

    ComponentMetadata componentMetadata = MetadataBuilder
        .buildComponentMetadata(WrongActivationMethodComponent.class);

    ComponentContainerInstance<Object> componentContainer = factory
        .createComponentContainer(componentMetadata);

    componentContainer.open();

    try {
      Assert.assertEquals(ComponentState.FAILED_PERMANENT,
          componentContainer.getResources()[0].getState());
    } finally {
      componentContainer.close();
    }
  }

  @Test
  public void testWrongReferenceConfiguration() {
    BundleContext bundleContext = componentContext.getBundleContext();
    ComponentContainerFactory factory = new ComponentContainerFactory(bundleContext);

    ComponentMetadata componentMetadata = MetadataBuilder
        .buildComponentMetadata(MultiRequirementAndCapabilityComponent.class);

    ComponentContainerInstance<Object> container = factory
        .createComponentContainer(componentMetadata);

    container.open();

    try {

      waitForTrueSupplied(() -> container.getResources().length == 1);

      Assert.assertNotEquals(ComponentState.FAILED, container.getResources()[0].getState());

      Hashtable<String, Object> configuration = new Hashtable<>();
      configuration.put("someRef.target", new String[] { "(WrongSyntax)" });
      updateConfiguration(container, configuration);

      waitForTrueSupplied(() -> ComponentState.FAILED == getComponentState(container));
      Assert.assertEquals(ComponentState.FAILED, getComponentState(container));

      configuration.put("someRef.target", new String[] { "(service.id>=0)" });
      updateConfiguration(container, configuration);

      waitForTrueSupplied(() -> ComponentState.FAILED != getComponentState(container));
      Assert.assertNotEquals(ComponentState.FAILED, getComponentState(container));
    } finally {
      container.close();
    }

  }

  private void updateConfiguration(final ComponentContainer<?> container,
      final Hashtable<String, Object> properties) {
    try {
      Configuration configuration = configAdmin
          .getConfiguration(container.getComponentMetadata().getConfigurationPid(), null);
      configurations.add(configuration);
      configuration.update(properties);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private <T> T waitForService(final Class<T> clazz) {
    return waitForService("(" + Constants.OBJECTCLASS + "=" + clazz.getName() + ")");
  }

  private <T> T waitForService(final String filterString) {
    Filter filter;
    try {
      filter = FrameworkUtil.createFilter(filterString);
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    }

    ServiceTracker<T, T> tracker = new ServiceTracker<>(
        componentContext.getBundleContext(), filter, null);
    tracker.open();
    try {
      long millisBefore = System.currentTimeMillis();
      T result = tracker.waitForService(WAITING_TIMEOUT);
      long millisAfter = System.currentTimeMillis();
      if (result == null) {
        Assert.fail("No service for component is available. Waited " + (millisAfter - millisBefore)
            + " ms.");
      }
      return result;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      tracker.close();
    }
  }

  private void waitForTrueSupplied(final Supplier<Boolean> supplier) {
    long startTime = System.currentTimeMillis();
    long endTime = startTime;
    boolean result = supplier.get();
    while (endTime - startTime < WAITING_TIMEOUT && !result) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      result = supplier.get();
      endTime = System.currentTimeMillis();
    }
    if (!result) {
      Assert.fail("Timeout");
    }
  }
}
