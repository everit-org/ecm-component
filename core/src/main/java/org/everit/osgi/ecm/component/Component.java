package org.everit.osgi.ecm.component;

import java.util.Dictionary;
import java.util.Hashtable;

import org.everit.osgi.ecm.component.internal.metatype.MetatypeProviderImpl;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.metatype.MetaTypeProvider;

public class Component<C> {

    private final BundleContext bundleContext;

    private final ComponentMetadata<C> componentMetadata;

    private final MetatypeProviderImpl<C> metatypeProviderImpl;

    private ServiceRegistration<MetaTypeProvider> metatypeProviderSR;

    public Component(ComponentMetadata<C> componentMetadata, BundleContext bundleContext) {
        this.componentMetadata = componentMetadata;
        this.bundleContext = bundleContext;

        metatypeProviderImpl = new MetatypeProviderImpl<C>(componentMetadata, bundleContext);

    }

    public void close() {
        if (metatypeProviderSR != null) {
            metatypeProviderSR.unregister();
        }
    }

    public void open() {
        String configurationPid = componentMetadata.getConfigurationPid();

        if (componentMetadata.isMetatype()) {
            boolean configurationFactory = componentMetadata.isConfigurationFactory();
            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            if (configurationFactory) {
                properties.put(MetaTypeProvider.METATYPE_FACTORY_PID, configurationPid);
            } else {
                properties.put(MetaTypeProvider.METATYPE_PID, configurationPid);
            }

            metatypeProviderSR = bundleContext
                    .registerService(MetaTypeProvider.class, metatypeProviderImpl, properties);
        }

    }
}
