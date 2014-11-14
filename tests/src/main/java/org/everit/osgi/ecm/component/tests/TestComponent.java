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

import java.util.Map;

import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Deactivate;
import org.everit.osgi.ecm.annotation.ReferenceConfigurationType;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.ThreeStateBoolean;
import org.everit.osgi.ecm.annotation.attribute.BooleanAttribute;
import org.everit.osgi.ecm.annotation.attribute.BooleanAttributes;
import org.everit.osgi.ecm.annotation.attribute.IntegerAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.ServiceHolder;
import org.osgi.service.cm.ManagedService;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@BooleanAttributes({ @BooleanAttribute(attributeId = "booleanArrayAttribute", multiple = ThreeStateBoolean.TRUE,
        defaultValue = { false, false, true }) })
@Service
public class TestComponent {

    private boolean[] booleanArrayAttribute;

    @BooleanAttribute(setter = "bindBooleanAttribute")
    private Boolean booleanAttribute;

    private int[] intArrayAttribute;

    @IntegerAttribute(defaultValue = 1)
    private int intAttribute;

    @StringAttribute
    private String[] stringArrayAttribute;

    @StringAttribute(dynamic = true)
    private String stringAttribute;

    @Activate
    public void activate(ComponentContext<TestComponent> componentContext) {
        Map<String, Object> properties = componentContext.getProperties();
        Object booleanArrayAttribute = properties.get("booleanArrayAttribute");
        this.booleanArrayAttribute = (boolean[]) booleanArrayAttribute;
    }

    public void bindBooleanAttribute(Boolean booleanAttribute) {
        this.booleanAttribute = booleanAttribute;
    }

    @Deactivate
    public void deactivate() {

    }

    public boolean[] getBooleanArrayAttribute() {
        return booleanArrayAttribute;
    }

    public Boolean getBooleanAttribute() {
        return booleanAttribute;
    }

    public int[] getIntArrayAttribute() {
        return intArrayAttribute;
    }

    public int getIntAttribute() {
        return intAttribute;
    }

    public String[] getStringArrayAttribute() {
        return stringArrayAttribute;
    }

    public String getStringAttribute() {
        return stringAttribute;
    }

    @ServiceRef(configurationType = ReferenceConfigurationType.CLAUSE, optional = true)
    public void setClauseReference(ServiceHolder<ManagedService> clauseReference) {
    }

    @IntegerAttribute
    public void setIntArrayAttribute(int[] intArrayAttribute) {
        this.intArrayAttribute = intArrayAttribute;
    }

    public void setIntAttribute(int intAttribute) {
        this.intAttribute = intAttribute;
    }

    @ServiceRef(dynamic = true)
    public void setSomeReference(ServiceHolder<ManagedService> someReference) {
    }

    public void setStringArrayAttribute(String[] stringArrayAttribute) {
        this.stringArrayAttribute = stringArrayAttribute;
    }

    public void setStringAttribute(String stringAttribute) {
        this.stringAttribute = stringAttribute;
    }
}
