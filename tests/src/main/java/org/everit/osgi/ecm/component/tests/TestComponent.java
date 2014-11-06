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

import java.util.Arrays;

import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Deactivate;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Service
public class TestComponent {

    @StringAttribute
    private String[] stringArrayAttribute;

    @StringAttribute
    private String stringAttribute;

    @Activate
    public void activate() {
        System.out.println("//////////////// activate called: " + stringAttribute + ", "
                + Arrays.toString(stringArrayAttribute));
    }

    @Deactivate
    public void deactivate() {
        System.out.println("---------------- Deactivate called");
    }

    public void setStringArrayAttribute(String[] stringArrayAttribute) {
        this.stringArrayAttribute = stringArrayAttribute;
    }

    public void setStringAttribute(String stringAttribute) {
        this.stringAttribute = stringAttribute;
    }
}
