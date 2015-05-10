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

import java.lang.invoke.MethodHandle;

import org.everit.osgi.capabilitycollector.AbstractCapabilityCollector;
import org.everit.osgi.capabilitycollector.BundleCapabilityCollector;
import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.everit.osgi.capabilitycollector.Suiting;
import org.everit.osgi.ecm.component.BundleCapabilityHolder;
import org.everit.osgi.ecm.component.ConfigurationException;
import org.everit.osgi.ecm.component.ri.internal.ComponentContextImpl;
import org.everit.osgi.ecm.component.ri.internal.ReferenceEventHandler;
import org.everit.osgi.ecm.metadata.BundleCapabilityReferenceMetadata;
import org.osgi.framework.wiring.BundleCapability;

/**
 * Helper class to manage {@link BundleCapability} reference lifecycles.
 *
 * @param <COMPONENT>
 *          The type of the Component implementation
 */
public class BundleCapabilityReferenceAttributeHelper<COMPONENT> extends
    ReferenceHelper<BundleCapability, COMPONENT, BundleCapabilityReferenceMetadata> {

  public BundleCapabilityReferenceAttributeHelper(
      final BundleCapabilityReferenceMetadata referenceMetadata,
      final ComponentContextImpl<COMPONENT> componentContext,
      final ReferenceEventHandler eventHandler)
      throws IllegalAccessException {
    super(referenceMetadata, componentContext, eventHandler);
  }

  @Override
  protected void bindInternal() {
    MethodHandle setterMethod = getSetterMethodHandle();
    if (setterMethod == null) {
      return;
    }

    Object[] parameterArray = resolveParameterArray();

    try {
      if (isArray()) {
        setterMethod.invoke(getComponentContext().getInstance(), (Object) parameterArray);
      } else {
        if (parameterArray.length == 0) {
          setterMethod.invoke(getComponentContext().getInstance(), null);
        } else {
          setterMethod.invoke(getComponentContext().getInstance(), parameterArray[0]);
        }
      }
    } catch (Throwable e) {
      getComponentContext().fail(
          new ConfigurationException("Error during updating reference: "
              + getReferenceMetadata().getReferenceId(), e), false);
    }
  }

  @Override
  protected AbstractCapabilityCollector<BundleCapability> createCollector(
      final ReferenceCapabilityConsumer consumer,
      final RequirementDefinition<BundleCapability>[] requirements) {

    return new BundleCapabilityCollector(getComponentContext().getBundleContext(),
        getReferenceMetadata().getNamespace(), requirements, consumer,
        getReferenceMetadata().getStateMask());
  }

  @Override
  public void free() {
    // Do nothing
  }

  private Object[] resolveParameterArray() {
    Suiting<BundleCapability>[] lSuitings = getSuitings();

    Object[] parameterArray;
    if (isHolder()) {
      parameterArray = new BundleCapabilityHolder[lSuitings.length];
    } else {
      parameterArray = new BundleCapability[lSuitings.length];
    }

    for (int i = 0; i < lSuitings.length; i++) {
      Suiting<BundleCapability> suiting = lSuitings[i];
      if (isHolder()) {
        RequirementDefinition<BundleCapability> requirement = suiting.getRequirement();
        BundleCapabilityHolder holder = new BundleCapabilityHolder(requirement.getRequirementId(),
            suiting.getCapability(), requirement.getAttributes());
        parameterArray[i] = holder;
      } else {
        parameterArray[i] = suiting.getCapability();
      }
    }

    return parameterArray;
  }

}
