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
package org.everit.osgi.ecm.component.ri.internal.resource;

import org.everit.osgi.ecm.component.resource.ComponentWire;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

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
