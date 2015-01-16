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

import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
@Service
public class IgnoredComponent {

    @StringAttribute(defaultValue = "Default")
    private String propertyWithDefaultValue;

    @StringAttribute(optional = true)
    private String propertyWithoutDefaultValue;

    public String getPropertyWithDefaultValue() {
        return propertyWithDefaultValue;
    }

    public String getPropertyWithoutDefaultValue() {
        return propertyWithoutDefaultValue;
    }

    public void setPropertyWithDefaultValue(final String propertyWithDefaultValue) {
        this.propertyWithDefaultValue = propertyWithDefaultValue;
    }

    public void setPropertyWithoutDefaultValue(final String propertyWithoutDefaultValue) {
        this.propertyWithoutDefaultValue = propertyWithoutDefaultValue;
    }
}
