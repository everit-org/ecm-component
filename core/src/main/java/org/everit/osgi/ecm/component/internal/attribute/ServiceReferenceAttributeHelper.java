/**
 * This file is part of Everit - ECM Component.
 *
 * Everit - ECM Component is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - ECM Component is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - ECM Component.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.ecm.component.internal.attribute;

import java.lang.reflect.Method;

import org.everit.osgi.capabilitycollector.AbstractCapabilityCollector;
import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.everit.osgi.capabilitycollector.ServiceReferenceCollector;
import org.everit.osgi.ecm.component.context.ComponentContext;
import org.everit.osgi.ecm.component.internal.ReferenceEventHandler;
import org.everit.osgi.ecm.metadata.ServiceReferenceMetadata;
import org.osgi.framework.ServiceReference;

public class ServiceReferenceAttributeHelper<S, COMPONENT> extends
        ReferenceHelper<ServiceReference<S>, COMPONENT> {

    private Method bindMethod;

    private final ServiceReferenceMetadata serviceReferenceMetadata;

    public ServiceReferenceAttributeHelper(ServiceReferenceMetadata referenceMetadata,
            ComponentContext<COMPONENT> componentContext, ReferenceEventHandler eventHandler) {
        super(referenceMetadata, componentContext, eventHandler);
        // TODO Auto-generated constructor stub
        serviceReferenceMetadata = referenceMetadata;
    }

    @Override
    protected void bindInternal() {
        // TODO Auto-generated method stub

    }

    @Override
    protected AbstractCapabilityCollector<ServiceReference<S>> createCollector(ReferenceCapabilityConsumer consumer) {
        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<S>>[] items = new RequirementDefinition[0];
        // TODO Auto-generated method stub
        @SuppressWarnings("unchecked")
        Class<S> serviceInterface = (Class<S>) serviceReferenceMetadata.getServiceInterface();
        return new ServiceReferenceCollector<S>(getComponentContext().getBundleContext(),
                serviceInterface, items, consumer, false);
    }

}
