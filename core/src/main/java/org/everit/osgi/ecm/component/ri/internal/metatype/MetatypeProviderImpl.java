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
package org.everit.osgi.ecm.component.ri.internal.metatype;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

public class MetatypeProviderImpl<C> implements MetaTypeProvider {

  private final BundleContext bundleContext;

  private final ComponentMetadata componentMetadata;

  private final String[] locales;

  public MetatypeProviderImpl(final ComponentMetadata componentMetadata,
      final BundleContext bundleContext) {
    this.componentMetadata = componentMetadata;
    this.bundleContext = bundleContext;

    this.locales = createLocales();
  }

  private String[] createLocales() {
    String localizationBase = componentMetadata.getLocalizationBase();
    if (localizationBase == null) {
      return null;
    }

    int lastIndexOfSlash = localizationBase.lastIndexOf('/');
    String path = localizationBase;
    String filePattern = localizationBase;
    if (lastIndexOfSlash > 0) {
      path = localizationBase.substring(0, lastIndexOfSlash);
      filePattern = filePattern.substring(lastIndexOfSlash + 1);
    }

    Bundle bundle = bundleContext.getBundle();
    BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);

    String propertiesPattern = "*.properties";
    Collection<String> resources = bundleWiring.listResources(path,
        filePattern + propertiesPattern,
        BundleWiring.LISTRESOURCES_LOCAL);

    if (resources.size() == 0) {
      return null;
    }

    int localizationBaseLength = localizationBase.length();
    Set<String> result = new HashSet<String>();
    int propertiesExtensionLength = propertiesPattern.length() - 1;
    for (String resource : resources) {
      String locale = resource.substring(localizationBaseLength, resource.length()
          - propertiesExtensionLength);
      if (locale.length() == 0) {
        result.add("");
      } else if (locale.startsWith("_")) {
        result.add(locale.substring(1));
      }
    }

    if (result.size() == 0) {
      return null;
    }
    return result.toArray(new String[result.size()]);
  }

  @Override
  public String[] getLocales() {
    return locales;
  }

  @Override
  public ObjectClassDefinition getObjectClassDefinition(final String id, final String localeString) {
    BundleWiring bundleWiring = bundleContext.getBundle().adapt(BundleWiring.class);
    ClassLoader classLoader = bundleWiring.getClassLoader();
    Localizer localizer;

    String localizationBase = componentMetadata.getLocalizationBase();
    if (locales == null) {
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

    ObjectClassDefinition objectClassDefinition = new ObjectClassDefinitionImpl<C>(
        componentMetadata, localizer,
        classLoader);
    return objectClassDefinition;
  }

}
