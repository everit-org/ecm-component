package org.everit.osgi.ecm.component.ri.internal.extender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.everit.osgi.ecm.component.ri.ComponentContainerFactory;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.annotation.headers.ProvideCapability;

@ProvideCapability(ns = "org.everit.osgi.ecm.component.tracker", value = "impl=ri", version = "1.0.0")
public class ECMCapabilityTracker extends BundleTracker<Bundle> {

    private final Map<Bundle, List<ComponentContainerInstance<?>>> activeComponentContainers = new ConcurrentHashMap<Bundle, List<ComponentContainerInstance<?>>>();

    public ECMCapabilityTracker(final BundleContext context) {
        super(context, Bundle.ACTIVE, null);
    }

    @Override
    public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiredOnlyToOtherTracker(wiring)) {
            return null;
        }
        List<BundleCapability> capabilities = wiring.getCapabilities("org.everit.osgi.ecm.component");

        if (capabilities == null || capabilities.size() == 0) {
            return null;
        }

        ComponentContainerFactory factory = new ComponentContainerFactory(bundle.getBundleContext());
        List<ComponentContainerInstance<?>> containers = new ArrayList<ComponentContainerInstance<?>>();
        for (BundleCapability capability : capabilities) {
            Object clazzAttribute = capability.getAttributes().get("class");
            if (clazzAttribute != null) {
                String clazz = String.valueOf(clazzAttribute);
                try {
                    Class<?> type = bundle.loadClass(clazz);
                    factory.createComponentContainer(Metadatab)
                } catch (Exception e) {
                    // TODO
                    e.printStackTrace();
                    return null;
                }
            } else {
                // TODO
                throw new RuntimeException("Class is not defined in capability: " + capability.toString());
            }
        }

        return bundle;
    };

    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {

    }

    private boolean wiredOnlyToOtherTracker(final BundleWiring wiring) {
        List<BundleWire> trackerWires = wiring.getRequiredWires("org.everit.osgi.ecm.component.tracker");

        if (trackerWires.size() == 0) {
            return false;
        }

        for (BundleWire bundleWire : trackerWires) {
            BundleCapability capability = bundleWire.getCapability();
            if (capability != null && capability.getRevision().getBundle().equals(context.getBundle())) {
                return false;
            }
        }

        return true;
    }
}
