/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.ecm.component.ri.internal.attribute;

import java.lang.reflect.Method;
import java.util.Arrays;
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

/**
 * Super class of classes that help managing the lifecycle and actions of references.
 *
 * @param <CAPABILITY>
 *          The type of the capability that the reference requires.
 * @param <COMPONENT>
 *          The type of the component implementation.
 * @param <METADATA>
 *          The type of the metadata class that belongs to the reference.
 */
public abstract class ReferenceHelper<CAPABILITY, COMPONENT, METADATA extends ReferenceMetadata> {

  /**
   * {@link CapabilityConsumer} implementation for references that call the necessary
   * {@link ReferenceEventHandler} of the component if a change happens.
   */
  protected class ReferenceCapabilityConsumer implements CapabilityConsumer<CAPABILITY> {

    @Override
    public void accept(final Suiting<CAPABILITY>[] pSuitings, final boolean pSatisfied) {
      suitings = pSuitings;
      satisfied = pSatisfied;
      ReferenceHelper<CAPABILITY, COMPONENT, METADATA> owner = ReferenceHelper.this;
      if (configurationUpdateFailure) {
        satisfiedNotificationSent = false;
        eventHandler.failedDuringConfigurationUpdate(owner);
      } else if (pSatisfied) {
        if (!satisfiedNotificationSent) {
          satisfiedNotificationSent = true;
          eventHandler.satisfied(owner);
        } else {
          if (referenceMetadata.isDynamic()) {
            eventHandler.updateDynamicWithoutSatisfactionChange(owner);
          } else {
            eventHandler.updateNonDynamic(owner);
          }
        }
      } else {
        if (satisfiedNotificationSent) {
          satisfiedNotificationSent = false;
          eventHandler.unsatisfied(owner);
        } else {
          eventHandler.updateDynamicWithoutSatisfactionChange(owner);
        }
      }
    }
  }

  private final boolean array;

  private final AbstractCapabilityCollector<CAPABILITY> collector;

  private final ComponentContextImpl<COMPONENT> componentContext;

  private boolean configurationUpdateFailure = false;

  private final ReferenceEventHandler eventHandler;

  private final boolean holder;

  private final METADATA referenceMetadata;

  private boolean satisfied = false;

  private boolean satisfiedNotificationSent = false;

  private final Method setterMethod;

  private Suiting<CAPABILITY>[] suitings;

  /**
   * Constructor of {@link ReferenceHelper}.
   *
   * @param referenceMetadata
   *          The metadata that describes the attributes of the reference.
   * @param componentContext
   *          The context of the component that the reference blongs to.
   * @param eventHandler
   *          The event handler that should be called if there is any change in the state of the
   *          reference.
   * @throws IllegalAccessException
   *           if there is an issue during resolving the setter method.
   */
  public ReferenceHelper(final METADATA referenceMetadata,
      final ComponentContextImpl<COMPONENT> componentContext,
      final ReferenceEventHandler eventHandler) throws IllegalAccessException {
    this.referenceMetadata = referenceMetadata;
    this.componentContext = componentContext;
    this.eventHandler = eventHandler;

    MethodDescriptor setterMethodDescriptor = referenceMetadata.getSetter();
    if (setterMethodDescriptor == null) {
      holder = false;
      setterMethod = null;
      array = false;
    } else {
      this.setterMethod = setterMethodDescriptor.locate(componentContext.getComponentType(),
          false);
      if (setterMethod == null) {
        throw new MetadataValidationException("Setter method '" + setterMethodDescriptor.toString()
            + "' could not be found for class " + componentContext.getComponentTypeName());
      }

      Class<?>[] parameterTypes = setterMethod.getParameterTypes();
      if ((parameterTypes.length != 1) || parameterTypes[0].isPrimitive()) {
        throw new MetadataValidationException("Setter method for reference '"
            + referenceMetadata.toString()
            + "' that is defined in the class '" + componentContext.getComponentTypeName()
            + "' must have one non-primitive parameter.");
      }

      if (AbstractReferenceHolder.class.isAssignableFrom(parameterTypes[0])) {
        holder = true;
        array = false;
      } else {
        if (parameterTypes[0].isArray()) {
          if (AbstractReferenceHolder.class
              .isAssignableFrom(parameterTypes[0].getComponentType())) {
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

    init();

    @SuppressWarnings("unchecked")
    RequirementDefinition<CAPABILITY>[] requirements = new RequirementDefinition[0];
    this.collector = createCollector(new ReferenceCapabilityConsumer(), requirements);
  }

  /**
   * Calls the setter method with the current referenced object(s).
   */
  public void bind() {
    try {
      if (setterMethod != null) {
        if (!array && (suitings.length > 1)) {
          getComponentContext().fail(new ConfigurationException(
              getReferenceMetadata().getAttributeId()
                  + ": Multiple references assigned to the reference while the setter"
                  + " method is not an array"),
              false);
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

  private RequirementDefinition<CAPABILITY>[] emptyRequirementDefinitionsArray() {
    @SuppressWarnings("unchecked")
    RequirementDefinition<CAPABILITY>[] result = new RequirementDefinition[0];
    return result;
  }

  private void failConfiguration(final String message, final Throwable e) {
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

  private void fillAttributesOfRequirementFromClause(final Map<String, Object> attributes,
      final Clause clauses) {

    Attribute[] parsedAttributes = clauses.getAttributes();
    for (Attribute attribute : parsedAttributes) {
      attributes.put(attribute.getName(), attribute.getValue());
    }
  }

  /**
   * Called when the component failes and every attached resource should be unattached. E.g.: unget
   * services that were previously got.
   */
  public abstract void free();

  private RequirementDefinition<CAPABILITY>[] generateRequirementDefinitions(
      final String[] requirementStringArray, final ReferenceConfigurationType configurationType) {

    RequirementDefinition<CAPABILITY>[] result = createRequirementDefinitionArray(
        requirementStringArray.length);

    for (int i = 0; i < requirementStringArray.length; i++) {
      String requirementString = requirementStringArray[i];

      Map<String, Object> attributes = new LinkedHashMap<>();
      String requirementId = "" + i;
      String filterString = requirementString;

      if (configurationType == ReferenceConfigurationType.CLAUSE) {
        if (requirementString == null) {
          failConfiguration("Value must be defined", null);
          return emptyRequirementDefinitionsArray();
        }
        try {
          Clause[] clauses = Parser.parseClauses(new String[] { requirementString });
          if (clauses != null) {
            fillAttributesOfRequirementFromClause(attributes, clauses[0]);
            requirementId = clauses[0].getName();
            filterString = clauses[0].getDirective("filter");
          }
        } catch (IllegalArgumentException e) {
          failConfiguration("Invalid clause: " + requirementString, e);
          return emptyRequirementDefinitionsArray();
        }
      }

      Filter filter = null;
      if (filterString != null) {
        try {
          filter = FrameworkUtil.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
          failConfiguration("Invalid OSGi filter expression: " + requirementString, e);
          return emptyRequirementDefinitionsArray();
        }
      }
      result[i] = new RequirementDefinition<>(requirementId, filter, attributes);

    }
    return result;
  }

  public ComponentContextImpl<COMPONENT> getComponentContext() {
    return componentContext;
  }

  public METADATA getReferenceMetadata() {
    return referenceMetadata;
  }

  public Method getSetterMethod() {
    return setterMethod;
  }

  public Suiting<CAPABILITY>[] getSuitings() {
    return suitings.clone();
  }

  /**
   * Called in the end of the constructor of {@link ReferenceHelper} superclass, but before any of
   * the abstract functions are called that must be implemented by the subclass.
   */
  protected void init() {
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
    updateConfiguration();
    collector.open();
  }

  private void replaceEmptyStringWithNullInRequirementsArray(
      final String[] requirementStringArray) {

    for (int i = 0; i < requirementStringArray.length; i++) {
      String requirementString = requirementStringArray[i];
      if ((requirementString != null) && "".equals(requirementString.trim())) {
        requirementStringArray[i] = null;
      }
    }
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
      return emptyRequirementDefinitionsArray();
    }

    replaceEmptyStringWithNullInRequirementsArray(requirementStringArray);

    ReferenceConfigurationType configurationType = referenceMetadata
        .getReferenceConfigurationType();

    return generateRequirementDefinitions(requirementStringArray, configurationType);
  }

  /**
   * Called when the configuration of the component is updated.
   */
  public void updateConfiguration() {
    RequirementDefinition<CAPABILITY>[] newRequirements = resolveRequirements();

    if (getComponentContext().isFailed()) {
      configurationUpdateFailure = true;
    } else {
      configurationUpdateFailure = false;
    }

    try {
      collector.updateRequirements(newRequirements);
    } catch (Throwable e) {
      failConfiguration(
          "Cannot update requirements on tracker: " + Arrays.toString(newRequirements), e);
      configurationUpdateFailure = true;
    }
  }
}
