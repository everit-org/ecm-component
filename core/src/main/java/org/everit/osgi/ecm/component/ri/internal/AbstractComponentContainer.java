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
package org.everit.osgi.ecm.component.ri.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.everit.osgi.ecm.component.ECMComponentConstants;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.component.ri.internal.metatype.MetatypeProviderImpl;
import org.everit.osgi.ecm.component.ri.internal.resource.ComponentRevisionImpl;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

public abstract class AbstractComponentContainer<C> implements MetaTypeProvider, ComponentContainerInstance<C> {

    private final BundleContext bundleContext;

    private final ComponentMetadata componentMetadata;

    private final MetatypeProviderImpl<C> metatypeProvider;

    public AbstractComponentContainer(final ComponentMetadata componentMetadata, final BundleContext bundleContext) {
        this.componentMetadata = componentMetadata;
        this.bundleContext = bundleContext;
        this.metatypeProvider = new MetatypeProviderImpl<C>(componentMetadata, bundleContext);
    }

    protected void addCommonServiceProperties(final Dictionary<String, Object> properties) {
        properties.put(ECMComponentConstants.SERVICE_PROP_COMPONENT_CLASS, componentMetadata.getType());
        properties.put(ECMComponentConstants.SERVICE_PROP_COMPONENT_NAME, this.metatypeProvider
                .getObjectClassDefinition(null, null).getName());
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public ComponentMetadata getComponentMetadata() {
        return componentMetadata;
    }

    @Override
    public String[] getLocales() {
        return metatypeProvider.getLocales();
    }

    @Override
    public ObjectClassDefinition getObjectClassDefinition(final String id, final String locale) {
        return metatypeProvider.getObjectClassDefinition(id, locale);
    }

    @Override
    public abstract ComponentRevisionImpl<C>[] getResources();

    @Override
    public synchronized Wire[] getWires() {
        ComponentRevisionImpl<C>[] componentRevisions = getResources();
        if (componentRevisions.length == 0) {
            return new Wire[0];
        }
        if (componentRevisions.length == 1) {
            List<Wire> wires = componentRevisions[0].getWires();
            return wires.toArray(new Wire[wires.size()]);
        }

        List<Wire> result = new ArrayList<Wire>();
        for (ComponentRevisionImpl<C> componentRevisionImpl : componentRevisions) {
            result.addAll(componentRevisionImpl.getWires());
        }
        return result.toArray(new Wire[result.size()]);
    }

    @Override
    public Wire[] getWiresByCapability(final Capability capability) {
        ComponentRevisionImpl<C>[] componentRevisions = getResources();
        if (componentRevisions.length == 0) {
            return new Wire[0];
        }
        if (componentRevisions.length == 1) {
            List<Wire> wires = componentRevisions[0].getWiresByCapability(capability);
            return wires.toArray(new Wire[wires.size()]);
        }

        List<Wire> result = new ArrayList<Wire>();
        for (ComponentRevisionImpl<C> componentRevisionImpl : componentRevisions) {
            result.addAll(componentRevisionImpl.getWiresByCapability(capability));
        }
        return result.toArray(new Wire[result.size()]);
    }

    @Override
    public Wire[] getWiresByRequirement(final Requirement requirement) {
        ComponentRevisionImpl<C>[] componentRevisions = getResources();
        if (componentRevisions.length == 0) {
            return new Wire[0];
        }
        if (componentRevisions.length == 1) {
            List<Wire> wires = componentRevisions[0].getWiresByRequirement(requirement);
            return wires.toArray(new Wire[wires.size()]);
        }

        List<Wire> result = new ArrayList<Wire>();
        for (ComponentRevisionImpl<C> componentRevisionImpl : componentRevisions) {
            result.addAll(componentRevisionImpl.getWiresByRequirement(requirement));
        }
        return result.toArray(new Wire[result.size()]);
    }
}
