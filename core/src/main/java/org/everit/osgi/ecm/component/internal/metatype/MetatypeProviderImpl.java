/**
 * This file is part of Everit - ECM Component.
 *
 * Everit - ECM Component is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - ECM Component is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - ECM Component.  If not, see <http://www.gnu.org/licenses/>.
 */
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
