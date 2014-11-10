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
package org.everit.osgi.ecm.component.ri.internal.attribute;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.felix.utils.manifest.Attribute;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.everit.osgi.capabilitycollector.AbstractCapabilityCollector;
import org.everit.osgi.capabilitycollector.CapabilityConsumer;
import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.everit.osgi.capabilitycollector.Suiting;
import org.everit.osgi.ecm.component.AbstractReferenceHolder;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.ri.internal.ReferenceEventHandler;
import org.everit.osgi.ecm.metadata.MetadataValidationException;
import org.everit.osgi.ecm.metadata.ReferenceConfigurationType;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

public abstract class ReferenceHelper<CAPABILITY, COMPONENT, METADATA extends ReferenceMetadata> {

    protected class ReferenceCapabilityConsumer implements CapabilityConsumer<CAPABILITY> {

        @Override
        public void accept(Suiting<CAPABILITY>[] pSuitings, Boolean pSatisfied) {
            suitings = pSuitings;
            satisfied = pSatisfied;
            if (pSatisfied) {
                if (!satisfiedNotificationSent) {
                    satisfiedNotificationSent = true;
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
                    satisfiedNotificationSent = false;
                    eventHandler.unsatisfied();
                }
            }
        }
    }

    private final boolean array;

    private final AbstractCapabilityCollector<CAPABILITY> collector;

    private final ComponentContext<COMPONENT> componentContext;

    private final ReferenceEventHandler eventHandler;

    private final boolean holder;

    private Object previousInstance = null;

    private final METADATA referenceMetadata;

    private volatile boolean satisfied = false;

    private volatile boolean satisfiedNotificationSent = false;

    private final MethodHandle setterMethodHandle;

    private volatile Suiting<CAPABILITY>[] suitings;

    public ReferenceHelper(METADATA referenceMetadata, ComponentContext<COMPONENT> componentContext,
            ReferenceEventHandler eventHandler) throws IllegalAccessException {
        this.referenceMetadata = referenceMetadata;
        this.componentContext = componentContext;
        this.eventHandler = eventHandler;
        RequirementDefinition<CAPABILITY>[] requirements = resolveRequirements();
        // TODO catch all runtime ex and null return

        this.collector = createCollector(new ReferenceCapabilityConsumer(), requirements);

        MethodDescriptor setterMethodDescriptor = referenceMetadata.getSetter();
        if (setterMethodDescriptor == null) {
            holder = false;
            setterMethodHandle = null;
            array = false;
        } else {
            Method setterMethod = setterMethodDescriptor.locate(componentContext.getComponentType(), false);
            if (setterMethod == null) {
                throw new MetadataValidationException("Setter method '" + setterMethodDescriptor.toString()
                        + "' could not be found for class " + componentContext.getComponentType());
            }

            Lookup lookup = MethodHandles.lookup();

            this.setterMethodHandle = lookup.unreflect(setterMethod);

            Class<?>[] parameterTypes = setterMethod.getParameterTypes();
            if (parameterTypes.length != 1 || parameterTypes[0].isPrimitive()) {
                throw new MetadataValidationException("Setter method for reference '" + referenceMetadata.toString()
                        + "' that is defined in the class '" + componentContext.getComponentType()
                        + "' must have one non-primitive parameter.");
            }

            if (AbstractReferenceHolder.class.isAssignableFrom(parameterTypes[0])) {
                holder = true;
                array = false;
            } else {
                if (parameterTypes[0].isArray()) {
                    if (AbstractReferenceHolder.class.isAssignableFrom(parameterTypes[0].getComponentType())) {
                        holder = true;
                    } else {
                        holder = false;
                    }
                    array = true;
                } else {
                    array = false;
                    holder = false;
                }
            }
        }
    }

    public void bind() {
        try {
            if (setterMethodHandle != null) {
                COMPONENT instance = componentContext.getInstance();
                if (previousInstance == null || !previousInstance.equals(instance)) {
                    previousInstance = instance;
                    setterMethodHandle.bindTo(instance);
                }
                bindInternal();
            }
        } catch (RuntimeException e) {
            componentContext.fail(e, false);
        }
    }

    protected abstract void bindInternal();

    public void close() {
        satisfied = false;
        collector.close();
    }

    protected abstract AbstractCapabilityCollector<CAPABILITY> createCollector(ReferenceCapabilityConsumer consumer,
            RequirementDefinition<CAPABILITY>[] items);

    private RequirementDefinition<CAPABILITY>[] createRequirementDefinitionArray(int n) {
        @SuppressWarnings("unchecked")
        RequirementDefinition<CAPABILITY>[] result = new RequirementDefinition[n];
        return result;
    }

    public ComponentContext<COMPONENT> getComponentContext() {
        return componentContext;
    }

    public METADATA getReferenceMetadata() {
        return referenceMetadata;
    }

    public MethodHandle getSetterMethodHandle() {
        return setterMethodHandle;
    }

    public Suiting<CAPABILITY>[] getSuitings() {
        return suitings.clone();
    }

    public boolean isArray() {
        return array;
    }

    public boolean isHolder() {
        return holder;
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public void open() {
        collector.open();
    }

    private RequirementDefinition<CAPABILITY>[] resolveRequirements() {
        String attributeId = referenceMetadata.getAttributeId();
        Map<String, Object> properties = componentContext.getProperties();
        Object requirementAttribute = properties.get(attributeId);
        if (requirementAttribute == null) {
            RequirementDefinition<CAPABILITY>[] result = createRequirementDefinitionArray(0);
            return result;
        }

        String[] requirementStringArray;
        if (requirementAttribute instanceof String) {
            requirementStringArray = new String[1];
            requirementStringArray[0] = (String) requirementAttribute;
        } else if (requirementAttribute instanceof String[]) {
            requirementStringArray = (String[]) requirementAttribute;
        } else {
            componentContext.fail(new ConfigurationException("Invalid type for attribute " + attributeId
                    + " in configuration defined by service pid  " + properties.get(Constants.SERVICE_PID)), false);
            return null;
        }

        ReferenceConfigurationType configurationType = referenceMetadata.getReferenceConfigurationType();

        RequirementDefinition<CAPABILITY>[] result = createRequirementDefinitionArray(requirementStringArray.length);

        for (int i = 0; i < requirementStringArray.length; i++) {
            String requirementString = requirementStringArray[i];
            if (requirementString != null && "".equals(requirementString.trim())) {
                requirementString = null;
            }
            Map<String, Object> attributes = new LinkedHashMap<>();
            String requirementId = "" + i;
            String filterString = requirementString;

            if (configurationType == ReferenceConfigurationType.CLAUSE) {
                if (requirementString == null) {
                    componentContext.fail(new ConfigurationException("Entry of " + referenceMetadata.getAttributeId()
                            + " of configuration " + componentContext.getProperties().get(Constants.SERVICE_PID)
                            + " is not a valid clause: " + requirementString), false);
                }
                try {
                    Clause[] clauses = Parser.parseClauses(new String[] { requirementString });
                    if (clauses != null) {
                        Attribute[] parsedAttributes = clauses[0].getAttributes();
                        for (Attribute attribute : parsedAttributes) {
                            attributes.put(attribute.getName(), attribute.getValue());
                        }
                        requirementId = clauses[0].getName();
                        filterString = clauses[0].getDirective("filter");
                    }
                } catch (IllegalArgumentException e) {
                    componentContext.fail(new RuntimeException("Entry of " + referenceMetadata.getAttributeId()
                            + " of configuration " + componentContext.getProperties().get(Constants.SERVICE_PID)
                            + " is not a valid clause: " + requirementString, e), false);
                    return null;
                }
            }

            Filter filter = null;
            if (filterString != null) {
                try {
                    filter = FrameworkUtil.createFilter(filterString);
                } catch (InvalidSyntaxException e) {
                    componentContext.fail(new RuntimeException("Entry of " + referenceMetadata.getAttributeId()
                            + " of configuration " + componentContext.getProperties().get(Constants.SERVICE_PID)
                            + " contains invalid filter: " + requirementString, e), false);
                    return null;
                }
            }
            result[i] = new RequirementDefinition<CAPABILITY>(requirementId, filter, attributes);

        }
        return result;
    }

    public void updateConfiguration() {
        RequirementDefinition<CAPABILITY>[] newRequirements = resolveRequirements();
        collector.updateRequirements(newRequirements);
    }
}
