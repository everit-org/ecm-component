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

import java.util.Map;

import org.everit.osgi.ecm.component.resource.ComponentRequirement;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.osgi.resource.Capability;

/**
 * {@link org.osgi.resource.Requirement} implementation for ECM based components.
 *
 * @param <COMPONENT>
 *          The type of the component implementation.
 * @param <CAPABILITY>
 *          The type of the {@link Capability} that this requirement accepts.
 */
public class ComponentRequirementImpl<COMPONENT, CAPABILITY extends Capability> implements
    ComponentRequirement<COMPONENT, CAPABILITY> {

  private final Map<String, Object> attributes;

  private final Class<CAPABILITY> capabilityType;

  private final Map<String, String> directives;

  private final String namespace;

  private final String requirementId;

  private final ComponentRevisionImpl<COMPONENT> resource;

  /**
   * Constructor.
   *
   * @param requirementId
   *          The id of the requirement that is normally the namespace part in the clause or if the
   *          component is a target, the id of the property and the index of the requirement in a
   *          bracket if the property cardinality is multiple. E.g.: myprop[0].
   * @param namespace
   *          The namespace of the requirement. If it is an OSGi service, the namespace is
   *          "osgi.service".
   * @param resource
   *          The component revision that the requirement belongs to.
   * @param directives
   *          The directives of the requirement.
   * @param attributes
   *          The attributes of the requirement.
   * @param capabilityType
   *          The type of the capability that the requirement needs.
   */
  public ComponentRequirementImpl(final String requirementId, final String namespace,
      final ComponentRevisionImpl<COMPONENT> resource, final Map<String, String> directives,
      final Map<String, Object> attributes, final Class<CAPABILITY> capabilityType) {
    this.requirementId = requirementId;
    this.namespace = namespace;
    this.resource = resource;
    this.directives = directives;
    this.attributes = attributes;
    this.capabilityType = capabilityType;
  }

  @Override
  public Class<CAPABILITY> getAcceptedCapabilityType() {
    return capabilityType;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Map<String, String> getDirectives() {
    return directives;
  }

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public String getRequirementId() {
    return requirementId;
  }

  @Override
  public ComponentRevision<COMPONENT> getResource() {
    return resource;
  }

  @Override
  public boolean isSatisfied() {
    return resource.getWiresByRequirement(this).size() > 0;
  }
}
