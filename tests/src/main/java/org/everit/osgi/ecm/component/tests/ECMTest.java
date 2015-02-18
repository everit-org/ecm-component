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

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

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
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.component.ri.ComponentContainerFactory;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
@StringAttributes({
        @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE,
                defaultValue = "junit4"),
        @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TEST_ID, defaultValue = "ECMTest")

})
@Service
public class ECMTest {

    private ComponentContext<ECMTest> componentContext;

    private ConfigurationAdmin configAdmin;

    @Activate
    public void activate(final ComponentContext<ECMTest> componentContext) {
        this.componentContext = componentContext;
    }

    @ServiceRef(defaultValue = "(service.id>=0)")
    public void setConfigAdmin(final ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    @Test
    @TestDuringDevelopment
    public void testBundleCapabilityTestComponent() {
        Configuration configuration = null;
        try {

            configuration = configAdmin.getConfiguration(BundleCapabilityTestComponent.class.getName(), null);

            Hashtable<String, Object> properties = new Hashtable<String, Object>();

            properties.put("bcArrayReference.target", new String[] {});
            properties.put("bcHolderReference.target", new String[] {});
            properties.put("bcReference.target", new String[] {});

            configuration.update(properties);

            waitForService(BundleCapabilityTestComponent.class);

            properties.put("bcArrayReference.target", new String[] { "(testAttribute=1)" });
            configuration.update(properties);
            Thread.sleep(1000);
            waitForService(BundleCapabilityTestComponent.class);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (configuration != null) {
                    configuration.delete();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Test
    public void testIgnoredComponent() {
        IgnoredComponent ignoredComponent = waitForService(IgnoredComponent.class);

        Assert.assertEquals("Default", ignoredComponent.getPropertyWithDefaultValue());
        Assert.assertEquals(null, ignoredComponent.getPropertyWithoutDefaultValue());
    }

    @Test
    public void testTestComponent() {
        try {
            Configuration configuration = configAdmin
                    .getConfiguration("org.everit.osgi.ecm.component.tests.TestComponent", null);

            Hashtable<String, Object> properties = new Hashtable<String, Object>();
            properties.put("booleanAttribute", true);
            properties.put("booleanArrayAttribute", new boolean[] { true });

            // Testing if one size array is passed to a non-multiple attribute
            properties.put("byteAttribute", new byte[] { 1 });
            properties.put("byteArrayAttribute", new byte[] { 1 });

            properties.put("charAttribute", 'a');
            properties.put("charArrayAttribute", new char[] { 'a' });

            properties.put("doubleAttribute", 1.1D);
            properties.put("doubleArrayAttribute", new double[] { 1.1D });

            properties.put("floatAttribute", 1.1F);
            properties.put("floatArrayAttribute", new float[] { 1.1F });

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

            configuration.update(properties);

            try {
                TestComponent testComponent = waitForService(TestComponent.class);

                Assert.assertTrue(testComponent.getBooleanAttribute());
                Assert.assertTrue(Arrays.equals(new boolean[] { true }, testComponent.getBooleanArrayAttribute()));

                Assert.assertEquals((byte) 1, testComponent.getByteAttribute());
                Assert.assertArrayEquals(new byte[] { 1 }, testComponent.getByteArrayAttribute());

                Assert.assertEquals('a', testComponent.getCharAttribute());
                Assert.assertArrayEquals(new char[] { 'a' }, testComponent.getCharArrayAttribute());

                Assert.assertTrue(1.1D == testComponent.getDoubleAttribute());
                Assert.assertTrue(Arrays.equals(new double[] { 1.1D }, testComponent.getDoubleArrayAttribute()));

                Assert.assertTrue(1.1F == testComponent.getFloatAttribute());
                Assert.assertTrue(Arrays.equals(new float[] { 1.1F }, testComponent.getFloatArrayAttribute()));

                Assert.assertTrue(1 == testComponent.getIntAttribute());
                Assert.assertTrue(Arrays.equals(new int[] { 1 }, testComponent.getIntArrayAttribute()));

                Assert.assertTrue(1L == testComponent.getLongAttribute());
                Assert.assertTrue(Arrays.equals(new long[] { 1L }, testComponent.getLongArrayAttribute()));

                Assert.assertTrue(1 == testComponent.getShortAttribute());
                Assert.assertTrue(Arrays.equals(new short[] { 1 }, testComponent.getShortArrayAttribute()));

                Assert.assertEquals("123456", testComponent.getPasswordAttribute());
                Assert.assertArrayEquals(new String[] { "123456" }, testComponent.getPasswordArrayAttribute());

                Assert.assertEquals("Hello World", testComponent.getStringAttribute());
                Assert.assertArrayEquals(new String[] { "Hello World" }, testComponent.getStringArrayAttribute());

            } finally {
                configuration.delete();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @TestDuringDevelopment
    public void testWrongActivateMethodComponent() {
        BundleContext bundleContext = componentContext.getBundleContext();
        ComponentContainerFactory factory = new ComponentContainerFactory(bundleContext);

        ComponentMetadata componentMetadata = MetadataBuilder
                .buildComponentMetadata(WrongActivationMethodComponent.class);

        ComponentContainerInstance<Object> componentContainer = factory.createComponentContainer(componentMetadata);

        componentContainer.open();

        try {
            Assert.assertEquals(ComponentState.FAILED_PERMANENT,
                    componentContainer.getResources()[0].getState());
        } finally {
            componentContainer.close();
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
            T result = tracker.waitForService(1000);
            if (result == null) {
                Assert.fail("No service for component is available");
            }
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            tracker.close();
        }
    }
}
