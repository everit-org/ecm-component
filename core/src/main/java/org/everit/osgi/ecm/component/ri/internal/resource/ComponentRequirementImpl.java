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

import java.util.Map;

import org.everit.osgi.ecm.component.resource.ComponentRequirement;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.osgi.resource.Capability;

public class ComponentRequirementImpl<C extends Capability> implements ComponentRequirement<C> {

    private final Map<String, Object> attributes;

    private final Class<C> capabilityType;

    private final Map<String, String> directives;

    private final String namespace;

    private final String requirementId;

    private final ComponentRevision resource;

    public ComponentRequirementImpl(final String requirementId, final String namespace,
            final ComponentRevision resource, final Map<String, String> directives,
            final Map<String, Object> attributes, final Class<C> capabilityType) {
        this.requirementId = requirementId;
        this.namespace = namespace;
        this.resource = resource;
        this.directives = directives;
        this.attributes = attributes;
        this.capabilityType = capabilityType;
    }

    @Override
    public Class<C> getAcceptedCapabilityType() {
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
    public ComponentRevision getResource() {
        return resource;
    }
}
