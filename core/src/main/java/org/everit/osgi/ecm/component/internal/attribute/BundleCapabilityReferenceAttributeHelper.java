/**
 * This file is part of Everit - ECM Component RI.
 *
 * Everit - ECM Component RI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - ECM Component RI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - ECM Component RI.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.ecm.component.internal.attribute;

import org.everit.osgi.capabilitycollector.AbstractCapabilityCollector;
import org.everit.osgi.capabilitycollector.BundleCapabilityCollector;
import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.internal.ReferenceEventHandler;
import org.everit.osgi.ecm.metadata.BundleCapabilityReferenceMetadata;
import org.osgi.framework.wiring.BundleCapability;

public class BundleCapabilityReferenceAttributeHelper<COMPONENT> extends
        ReferenceHelper<BundleCapability, COMPONENT> {

    private final BundleCapabilityReferenceMetadata bundleCapabilityReferenceMetadata;

    public BundleCapabilityReferenceAttributeHelper(BundleCapabilityReferenceMetadata referenceMetadata,
            ComponentContext<COMPONENT> componentContext, ReferenceEventHandler eventHandler) {
        super(referenceMetadata, componentContext, eventHandler);
        bundleCapabilityReferenceMetadata = referenceMetadata;
    }

    @Override
    protected void bindInternal() {

    }

    @Override
    protected AbstractCapabilityCollector<BundleCapability> createCollector(ReferenceCapabilityConsumer consumer) {
        // TODO handle custom bundle states if we want

        @SuppressWarnings("unchecked")
        RequirementDefinition<BundleCapability>[] requirements = new RequirementDefinition[0];
        return new BundleCapabilityCollector(getComponentContext().getBundleContext(),
                bundleCapabilityReferenceMetadata.getNamespace(), requirements, consumer,
                bundleCapabilityReferenceMetadata.getStateMask());
    }

}
