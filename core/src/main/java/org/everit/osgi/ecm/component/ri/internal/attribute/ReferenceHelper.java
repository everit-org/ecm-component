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

import org.apache.felix.utils.manifest.Attribute;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.everit.osgi.capabilitycollector.AbstractCapabilityCollector;
import org.everit.osgi.capabilitycollector.CapabilityConsumer;
import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.everit.osgi.capabilitycollector.Suiting;
import org.everit.osgi.ecm.component.AbstractReferenceHolder;
import org.everit.osgi.ecm.component.ConfigurationException;
import org.everit.osgi.ecm.component.ri.internal.ComponentContextImpl;
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

    private final ReferenceHelper<CAPABILITY, COMPONENT, METADATA> owner;

    public ReferenceCapabilityConsumer(final ReferenceHelper<CAPABILITY, COMPONENT, METADATA> owner) {
      this.owner = owner;
    }

    @Override
    public void accept(final Suiting<CAPABILITY>[] pSuitings, final Boolean pSatisfied) {
      suitings = pSuitings;
      satisfied = pSatisfied;
      if (pSatisfied) {
        if (!satisfiedNotificationSent) {
          satisfiedNotificationSent = true;
          eventHandler.satisfied(owner);
        } else {
          if (referenceMetadata.isDynamic()) {
            eventHandler.updateWithoutSatisfactionChange(owner);
            bind();
          } else {
            eventHandler.updateNonDynamic(owner);
          }
        }
      } else {
        if (satisfiedNotificationSent) {
          satisfiedNotificationSent = false;
          eventHandler.unsatisfied(owner);
        } else {
          eventHandler.updateWithoutSatisfactionChange(owner);
        }
      }
    }
  }

  private final boolean array;

  private final AbstractCapabilityCollector<CAPABILITY> collector;

  private final ComponentContextImpl<COMPONENT> componentContext;

  private final ReferenceEventHandler eventHandler;

  private final boolean holder;

  private Object previousInstance = null;

  private final METADATA referenceMetadata;

  private volatile boolean satisfied = false;

  private volatile boolean satisfiedNotificationSent = false;

  private final MethodHandle setterMethodHandle;

  private volatile Suiting<CAPABILITY>[] suitings;

  public ReferenceHelper(final METADATA referenceMetadata,
      final ComponentContextImpl<COMPONENT> componentContext,
      final ReferenceEventHandler eventHandler) throws IllegalAccessException {
    this.referenceMetadata = referenceMetadata;
    this.componentContext = componentContext;
    this.eventHandler = eventHandler;

    MethodDescriptor setterMethodDescriptor = referenceMetadata.getSetter();
    if (setterMethodDescriptor == null) {
      holder = false;
      setterMethodHandle = null;
      array = false;
    } else {
      Method setterMethod = setterMethodDescriptor.locate(componentContext.getComponentType(),
          false);
      if (setterMethod == null) {
        throw new MetadataValidationException("Setter method '" + setterMethodDescriptor.toString()
            + "' could not be found for class " + componentContext.getComponentType());
      }

      Lookup lookup = MethodHandles.lookup();

      this.setterMethodHandle = lookup.unreflect(setterMethod);

      Class<?>[] parameterTypes = setterMethod.getParameterTypes();
      if ((parameterTypes.length != 1) || parameterTypes[0].isPrimitive()) {
        throw new MetadataValidationException("Setter method for reference '"
            + referenceMetadata.toString()
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

    RequirementDefinition<CAPABILITY>[] requirements = resolveRequirements();

    if (requirements == null) {
      @SuppressWarnings("unchecked")
      RequirementDefinition<CAPABILITY>[] lRequirements = new RequirementDefinition[0];
      requirements = lRequirements;
    }

    this.collector = createCollector(new ReferenceCapabilityConsumer(this), requirements);
  }

  public void bind() {
    try {
      if (setterMethodHandle != null) {
        if (!array && (suitings.length > 1)) {
          getComponentContext().fail(new ConfigurationException(
              getReferenceMetadata().getAttributeId()
                  + ": Multiple references assigned to the reference while the setter"
                  + " method is not an array"),
              false);
        }

        COMPONENT instance = componentContext.getInstance();
        if ((previousInstance == null) || !previousInstance.equals(instance)) {
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

  protected abstract AbstractCapabilityCollector<CAPABILITY> createCollector(
      ReferenceCapabilityConsumer consumer,
      RequirementDefinition<CAPABILITY>[] items);

  private RequirementDefinition<CAPABILITY>[] createRequirementDefinitionArray(final int n) {
    @SuppressWarnings("unchecked")
    RequirementDefinition<CAPABILITY>[] result = new RequirementDefinition[n];
    return result;
  }

  private void failConfiguration(final String message, final Exception e) {
    String attributeId = referenceMetadata.getAttributeId();
    Map<String, Object> properties = componentContext.getProperties();
    String servicePid = String.valueOf(properties.get(Constants.SERVICE_PID));
    String finalMessage = "Error in configuration of attribute '" + attributeId
        + "' in '" + servicePid + "': " + message;
    if (e != null) {
      componentContext.fail(new ConfigurationException(finalMessage, e), false);
    } else {
      componentContext.fail(new ConfigurationException(finalMessage), false);
    }
  }

  public ComponentContextImpl<COMPONENT> getComponentContext() {
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

  public boolean isOpened() {
    return collector.isOpened();
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
      failConfiguration("Only String and String[] is accepted: "
          + requirementAttribute.getClass().getCanonicalName(), null);
      return null;
    }

    for (int i = 0; i < requirementStringArray.length; i++) {
      String requirementString = requirementStringArray[i];
      if ((requirementString != null) && "".equals(requirementString.trim())) {
        requirementStringArray[i] = null;
      }
    }

    ReferenceConfigurationType configurationType = referenceMetadata
        .getReferenceConfigurationType();

    RequirementDefinition<CAPABILITY>[] result = createRequirementDefinitionArray(requirementStringArray.length);

    for (int i = 0; i < requirementStringArray.length; i++) {
      String requirementString = requirementStringArray[i];

      Map<String, Object> attributes = new LinkedHashMap<>();
      String requirementId = "" + i;
      String filterString = requirementString;

      if (configurationType == ReferenceConfigurationType.CLAUSE) {
        if (requirementString == null) {
          failConfiguration("Value must be defined", null);
          return null;
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
          failConfiguration("Invalid clause: " + requirementString, e);
          return null;
        }
      }

      Filter filter = null;
      if (filterString != null) {
        try {
          filter = FrameworkUtil.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
          failConfiguration("Invalid OSGi filter expression: " + requirementString, e);
          return null;
        }
      }
      result[i] = new RequirementDefinition<CAPABILITY>(requirementId, filter, attributes);

    }
    return result;
  }

  public void updateConfiguration() {
    RequirementDefinition<CAPABILITY>[] newRequirements = resolveRequirements();
    if (newRequirements == null) {
      // Means that resolving failed so we must return
      return;
    }

    try {
      collector.updateRequirements(newRequirements);
    } catch (RuntimeException e) {
      failConfiguration("Cannot update requirements on tracker: " + newRequirements, e);
    }
  }
}
