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

import java.lang.reflect.Method;

import org.everit.osgi.capabilitycollector.AbstractCapabilityCollector;
import org.everit.osgi.capabilitycollector.CapabilityConsumer;
import org.everit.osgi.capabilitycollector.Suiting;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.internal.ReferenceEventHandler;
import org.everit.osgi.ecm.metadata.MetadataValidationException;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;

public abstract class ReferenceHelper<CAPABILITY, COMPONENT> {

    protected class ReferenceCapabilityConsumer implements CapabilityConsumer<CAPABILITY> {

        @Override
        public void accept(Suiting<CAPABILITY>[] pSuitings, Boolean pSatisfied) {
            suitings = pSuitings;
            satisfied = pSatisfied;
            if (pSatisfied) {
                if (!satisfiedNotificationSent) {
                    eventHandler.satisfied();
                } else {
                    if (referenceMetadata.isDynamic()) {
                        bind();
                    } else {
                        eventHandler.changedNonDynamic();
                    }
                }
            } else {
                if (satisfiedNotificationSent) {
                    eventHandler.unsatisfied();
                }
            }
        }
    }

    private final AbstractCapabilityCollector<CAPABILITY> collector;

    private final ComponentContext<COMPONENT> componentContext;

    private final ReferenceEventHandler eventHandler;

    private final boolean holder;

    private final ReferenceMetadata referenceMetadata;

    private volatile boolean satisfied = false;

    private volatile boolean satisfiedNotificationSent = false;

    private final Method setter;

    private volatile Suiting<CAPABILITY>[] suitings;

    public ReferenceHelper(ReferenceMetadata referenceMetadata, ComponentContext<COMPONENT> componentContext,
            ReferenceEventHandler eventHandler) {
        this.referenceMetadata = referenceMetadata;
        this.componentContext = componentContext;
        this.eventHandler = eventHandler;
        this.collector = createCollector(new ReferenceCapabilityConsumer());

        MethodDescriptor setterMethodDescriptor = referenceMetadata.getSetter();
        if (setterMethodDescriptor == null) {
            holder = false;
            setter = null;
        } else {
            Method setterMethod = setterMethodDescriptor.locate(componentContext.getComponentType(), false);
            if (setterMethod == null) {
                throw new MetadataValidationException("Setter method '" + setterMethodDescriptor.toString()
                        + "' could not be found for class " + componentContext.getComponentType());
            }
            Class<?>[] parameterTypes = setterMethod.getParameterTypes();
            if (parameterTypes.length != 1 || parameterTypes[0].isPrimitive()) {
                throw new MetadataValidationException("Setter method for reference '" + referenceMetadata.toString()
                        + "' that is defined in the class '" + componentContext.getComponentType()
                        + "' must have one non-primitive parameter.");
            }
        }
    }

    public void bind() {
        try {
            bindInternal();
        } catch (RuntimeException e) {
            componentContext.fail(e, false);
        }
    }

    protected abstract void bindInternal();

    public void close() {
        satisfied = false;
        collector.close();
    }

    protected abstract AbstractCapabilityCollector<CAPABILITY> createCollector(ReferenceCapabilityConsumer consumer);

    public ComponentContext<COMPONENT> getComponentContext() {
        return componentContext;
    }

    public Suiting<CAPABILITY>[] getSuitings() {
        return suitings.clone();
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public void open() {
        collector.open();
    }
}
