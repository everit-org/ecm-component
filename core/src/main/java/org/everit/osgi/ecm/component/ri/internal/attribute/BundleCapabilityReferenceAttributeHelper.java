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
