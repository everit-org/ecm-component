package org.everit.osgi.ecm.component.tests;

import org.everit.osgi.ecm.annotation.metadatabuilder.MetadataBuilder;
import org.everit.osgi.ecm.component.Component;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ECMTestActivator implements BundleActivator {

    private Component<AnnotatedClass> component;

    @Override
    public void start(BundleContext context) throws Exception {
        ComponentMetadata<AnnotatedClass> componentMetadata = MetadataBuilder
                .buildComponentMetadata(AnnotatedClass.class);

        component = new Component<AnnotatedClass>(componentMetadata, context);
        component.open();

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        component.close();
    }

}
