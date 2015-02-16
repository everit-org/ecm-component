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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.everit.osgi.capabilitycollector.Suiting;
import org.everit.osgi.ecm.component.resource.ComponentRequirement;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.metadata.BundleCapabilityReferenceMetadata;
import org.everit.osgi.ecm.metadata.ReferenceConfigurationType;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class ComponentRevisionImpl implements ComponentRevision {

    public static class Builder {

        private Throwable cause = null;

        private Thread processingThread = null;

        private Map<String, Object> properties;

        private ComponentState state = ComponentState.STOPPED;

        private final Map<ReferenceMetadata, Suiting<?>[]> suitingsByAttributeIds =
                new HashMap<ReferenceMetadata, Suiting<?>[]>();

        public Builder(final Map<String, Object> properties) {
            this.properties = properties;
        }

        public synchronized void active() {
            this.state = ComponentState.ACTIVE;
            this.processingThread = null;

        }

        public synchronized ComponentRevisionImpl build() {
            return new ComponentRevisionImpl(this);
        }

        public synchronized void fail(final Throwable cause, final boolean permanent) {
            this.cause = cause;
            this.processingThread = null;
            if (permanent) {
                this.state = ComponentState.FAILED_PERMANENT;
            } else {
                this.state = ComponentState.FAILED;
            }
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public ComponentState getState() {
            return state;
        }

        public synchronized void starting() {
            this.state = ComponentState.STARTING;
            this.cause = null;
            this.processingThread = Thread.currentThread();
        }

        public synchronized void stopped(final ComponentState targetState) {
            if (targetState != ComponentState.UPDATING_CONFIGURATION) {
                this.processingThread = null;
            }
            if (targetState == ComponentState.STOPPED || targetState == ComponentState.UPDATING_CONFIGURATION) {
                cause = null;
            }
            state = targetState;
        }

        public synchronized void stopping() {
            this.state = ComponentState.STOPPING;
            this.processingThread = Thread.currentThread();
        }

        public synchronized void unsatisfied() {
            this.state = ComponentState.UNSATISFIED;
            this.processingThread = null;
            this.cause = null;
        }

        public synchronized void updateProperties(final Map<String, Object> properties) {
            this.properties = properties;
            this.state = ComponentState.UPDATING_CONFIGURATION;
        }

        public synchronized void updateSuitingsForAttribute(final ReferenceMetadata referenceMetadata,
                final Suiting<?>[] suitings) {
            suitingsByAttributeIds.put(referenceMetadata, suitings);
        }
    }

    private static final class ComponentRequirementComparator implements Comparator<Requirement> {
        @Override
        public int compare(final Requirement o1, final Requirement o2) {
            return ((ComponentRequirement<?>) o1).getRequirementId().compareTo(
                    ((ComponentRequirement<?>) o2).getRequirementId());
        }
    }

    private static final ComponentRequirementComparator REQUIREMENT_COMPARATOR = new ComponentRequirementComparator();

    private final Throwable cause;

    private final Thread processingThread;

    private final Map<String, Object> properties;

    private final Map<String, List<Requirement>> requirementsByNamespace;

    private final ComponentState state;

    private ComponentRevisionImpl(final Builder builder) {
        this.state = builder.state;
        this.processingThread = builder.processingThread;
        this.cause = builder.cause;
        this.properties = builder.properties;

        this.requirementsByNamespace = evaluateRequirements(builder);
    }

    private Map<String, List<Requirement>> evaluateRequirements(final Builder builder) {
        Set<Entry<ReferenceMetadata, Suiting<?>[]>> suitingEntries = builder.suitingsByAttributeIds.entrySet();
        Iterator<Entry<ReferenceMetadata, Suiting<?>[]>> iterator = suitingEntries.iterator();

        Map<String, List<Requirement>> lRequirementsByNamespace =
                new TreeMap<String, List<Requirement>>();

        while (iterator.hasNext()) {
            Map.Entry<ReferenceMetadata, Suiting<?>[]> entry = iterator.next();
            ReferenceMetadata referenceMetadata = entry.getKey();
            String referenceId = referenceMetadata.getReferenceId();
            String namespace = "osgi.service";
            if (referenceMetadata instanceof BundleCapabilityReferenceMetadata) {
                namespace = ((BundleCapabilityReferenceMetadata) referenceMetadata).getNamespace();
            }

            List<Requirement> requirementsOfNS = lRequirementsByNamespace.get(namespace);
            if (requirementsOfNS == null) {
                requirementsOfNS = new ArrayList<Requirement>();
                lRequirementsByNamespace.put(namespace, requirementsOfNS);
            }

            Suiting<?>[] suitings = entry.getValue();
            for (Suiting<?> suiting : suitings) {
                String fullRequirementId = referenceId;
                if (referenceMetadata.isMultiple()
                        || referenceMetadata.getReferenceConfigurationType() == ReferenceConfigurationType.CLAUSE) {

                    fullRequirementId += "\\" + suiting.getRequirement().getRequirementId();
                }
                Object capabilityObject = suiting.getCapability();
                Capability capability = null;
                if (capabilityObject != null) {
                    if (capabilityObject instanceof ServiceReference<?>) {
                        capability = new ServiceCapabilityImpl((ServiceReference<?>) capabilityObject);
                    } else {
                        // This must be BundleCapability than
                        capability = (BundleCapability) capabilityObject;
                    }
                }

                Map<String, String> directives = new HashMap<String, String>();
                RequirementDefinition<?> requirement = suiting.getRequirement();
                if (requirement.getFilter() != null) {

                    directives.put("filter", requirement.getFilter().toString());
                }

                Capability[] wiredCapabilities;
                if (capability == null) {
                    wiredCapabilities = new Capability[0];
                } else {
                    wiredCapabilities = new Capability[] { capability };
                }
                requirementsOfNS.add(new ComponentRequirementImpl<Capability>(fullRequirementId, namespace, this,
                        Collections.unmodifiableMap(directives),
                        Collections.unmodifiableMap(new HashMap<String, Object>(requirement.getAttributes())),
                        wiredCapabilities));
            }
        }

        // Making all set readonly
        for (Entry<String, List<Requirement>> entry : lRequirementsByNamespace.entrySet()) {
            Collections.sort(entry.getValue(), REQUIREMENT_COMPARATOR);
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }

        return Collections.unmodifiableMap(lRequirementsByNamespace);
    }

    @Override
    public List<Capability> getCapabilities(final String namespace) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public Thread getProcessingThread() {
        return processingThread;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public List<Requirement> getRequirements(final String namespace) {
        List<Requirement> result;
        if (namespace != null) {
            result = requirementsByNamespace.get(namespace);
            if (result == null) {
                result = Collections.emptyList();
            }
        } else {
            result = new ArrayList<Requirement>();
            Collection<List<Requirement>> values = requirementsByNamespace.values();
            for (List<Requirement> requirements : values) {
                result.addAll(requirements);
            }
            result = Collections.unmodifiableList(result);
        }
        return result;
    }

    @Override
    public ComponentState getState() {
        return state;
    }

}
