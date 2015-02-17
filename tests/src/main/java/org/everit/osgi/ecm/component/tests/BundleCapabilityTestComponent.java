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

import org.everit.osgi.ecm.annotation.BundleCapabilityRef;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.ReferenceConfigurationType;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.component.BundleCapabilityHolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Service
public class BundleCapabilityTestComponent {

    @BundleCapabilityRef(namespace = "testNS", stateMask = Bundle.ACTIVE | Bundle.STARTING,
            setter = "setBcArrayReference")
    private BundleCapability[] bcArrayReference;

    @BundleCapabilityRef(namespace = "testNS", configurationType = ReferenceConfigurationType.CLAUSE)
    private BundleCapability bcClauseReference;

    @BundleCapabilityRef(namespace = "testNS")
    private BundleCapabilityHolder bcHolderReference;

    @BundleCapabilityRef(namespace = "testNS")
    private BundleCapability bcReference;

    public BundleCapability[] getBcArrayReference() {
        return bcArrayReference;
    }

    public BundleCapabilityHolder getBcHolderReference() {
        return bcHolderReference;
    }

    public BundleCapability getBcReference() {
        return bcReference;
    }

    public void setBcArrayReference(final BundleCapability[] bcArrayReference) {
        System.out.println("///////////  " + Arrays.toString(bcArrayReference));
        this.bcArrayReference = bcArrayReference;
    }

    public void setBcClauseReference(final BundleCapability bcClauseReference) {
        this.bcClauseReference = bcClauseReference;
    }

    public void setBcHolderReference(final BundleCapabilityHolder bcHolderReference) {
        this.bcHolderReference = bcHolderReference;
    }

    public void setBcReference(final BundleCapability bcReference) {
        this.bcReference = bcReference;
    }

}
