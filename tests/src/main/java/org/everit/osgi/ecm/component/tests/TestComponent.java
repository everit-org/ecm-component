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
import org.everit.osgi.ecm.annotation.ReferenceConfigurationType;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.component.ServiceHolder;
import org.osgi.service.cm.ManagedService;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Service
public class TestComponent {

    private ManagedService someReference;

    @StringAttribute
    private String[] stringArrayAttribute;

    @StringAttribute(dynamic = true)
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

    @ServiceRef(configurationType = ReferenceConfigurationType.CLAUSE, optional = true)
    public void setClauseReference(ServiceHolder<ManagedService> clauseReference) {
        System.out.println("-----------------Setter called: " + clauseReference.toString());
    }

    @ServiceRef
    public void setSomeReference(ServiceHolder<ManagedService> someReference) {
        System.out.println("---------- Setter called: " + someReference.toString());
        this.someReference = someReference.getService();
    }

    public void setStringArrayAttribute(String[] stringArrayAttribute) {
        System.out.println("-------Setter stringArrayAttribute: " + stringArrayAttribute);
        this.stringArrayAttribute = stringArrayAttribute;
    }

    public void setStringAttribute(String stringAttribute) {
        System.out.println("-------Setter stringAttribute: " + stringAttribute);
        this.stringAttribute = stringAttribute;
    }
}
