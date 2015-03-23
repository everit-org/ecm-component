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
package org.everit.osgi.ecm.component.ri.internal.resource;

import org.everit.osgi.ecm.component.resource.ComponentWire;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Implementation of {@link ComponentWire}.
 */
public class ComponentWireImpl implements ComponentWire {

  private final Capability capability;

  private final Requirement requirement;

  public ComponentWireImpl(final Requirement requirement, final Capability capability) {
    this.requirement = requirement;
    this.capability = capability;
  }

  @Override
  public Capability getCapability() {
    return capability;
  }

  @Override
  public Resource getProvider() {
    return capability.getResource();
  }

  @Override
  public Requirement getRequirement() {
    return requirement;
  }

  @Override
  public Resource getRequirer() {
    return requirement.getResource();
  }

}
