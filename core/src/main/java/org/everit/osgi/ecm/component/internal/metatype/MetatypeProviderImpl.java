package org.everit.osgi.ecm.component.internal.metatype;

import java.util.Locale;
import java.util.ResourceBundle;

import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

public class MetatypeProviderImpl<C> implements MetaTypeProvider {

    private final BundleContext bundleContext;

    private final ComponentMetadata<C> componentMetadata;

    public MetatypeProviderImpl(ComponentMetadata<C> componentMetadata, BundleContext bundleContext) {
        this.componentMetadata = componentMetadata;
        this.bundleContext = bundleContext;
    }

    @Override
    public String[] getLocales() {
        // TODO handle locales if necessary
        return null;
    }

    @Override
    public ObjectClassDefinition getObjectClassDefinition(String id, String localeString) {
        BundleWiring bundleWiring = bundleContext.getBundle().adapt(BundleWiring.class);
        ClassLoader classLoader = bundleWiring.getClassLoader();
        Localizer localizer;

        String localizationBase = componentMetadata.getLocalizationBase();
        if (localizationBase == null) {
            localizer = new Localizer(null);
        } else {
            Locale locale;
            if (localeString == null) {
                locale = Locale.getDefault();
            } else {
                String[] localeParts = localeString.split("_");
                if (localeParts.length == 1) {
                    locale = new Locale(localeParts[0]);
                } else if (localeParts.length == 2) {
                    locale = new Locale(localeParts[0], localeParts[1]);
                } else {
                    locale = new Locale(localeParts[0], localeParts[1], localeParts[2]);
                }
            }
            localizer = new Localizer(ResourceBundle.getBundle(localizationBase, locale, classLoader));

        }

        ObjectClassDefinition objectClassDefinition = new ObjectClassDefinitionImpl<C>(componentMetadata, localizer,
                classLoader);
        return objectClassDefinition;
    }

}
