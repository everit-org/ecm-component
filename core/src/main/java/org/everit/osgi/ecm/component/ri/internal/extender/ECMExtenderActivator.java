package org.everit.osgi.ecm.component.ri.internal.extender;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ECMExtenderActivator implements BundleActivator {

    private ECMCapabilityTracker tracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        tracker = new ECMCapabilityTracker(context);
        tracker.open();
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        tracker.close();
    }

}
