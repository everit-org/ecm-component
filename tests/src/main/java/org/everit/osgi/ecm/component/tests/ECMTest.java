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
import org.everit.osgi.ecm.component.ComponentContext;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
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
@TestDuringDevelopment
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

            properties.put("intAttribute", 1);
            properties.put("intArrayAttribute", new int[] { 1 });

            properties.put("stringAttribute", "Hello World");
            properties.put("stringArrayAttribute", new String[] { "Hello World" });

            configuration.update(properties);

            BundleContext bundleContext = componentContext.getBundleContext();
            ServiceTracker<TestComponent, TestComponent> testComponentTracker = new ServiceTracker<TestComponent, TestComponent>(
                    bundleContext, TestComponent.class, null);
            testComponentTracker.open();

            try {
                TestComponent testComponent = testComponentTracker.waitForService(1000);
                Assert.assertEquals((byte) 1, testComponent.getByteAttribute());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                testComponentTracker.close();
                configuration.delete();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
