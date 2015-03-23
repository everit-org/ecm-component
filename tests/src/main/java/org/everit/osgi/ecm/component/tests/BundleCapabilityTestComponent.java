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
package org.everit.osgi.ecm.component.tests;

import org.everit.osgi.ecm.annotation.BundleCapabilityRef;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.ReferenceConfigurationType;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.component.BundleCapabilityHolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;

/**
 * Component for testing {@link BundleCapability} references.
 */
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
    return bcArrayReference.clone();
  }

  public BundleCapability getBcClauseReference() {
    return bcClauseReference;
  }

  public BundleCapabilityHolder getBcHolderReference() {
    return bcHolderReference;
  }

  public BundleCapability getBcReference() {
    return bcReference;
  }

  public void setBcArrayReference(final BundleCapability[] bcArrayReference) {
    this.bcArrayReference = bcArrayReference.clone();
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
