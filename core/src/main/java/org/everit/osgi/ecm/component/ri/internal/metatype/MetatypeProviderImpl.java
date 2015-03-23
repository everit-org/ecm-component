/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * Implementation of {@link MetaTypeProvider} for ECM based Component Containers.
 *
 * @param <C>
 *          Type of the Component implementation.
 */
public class MetatypeProviderImpl<C> implements MetaTypeProvider {

  private static final String LOCALE_SEPARATOR = "_";

  private final BundleContext bundleContext;

  private final ComponentMetadata componentMetadata;

  private final String[] locales;

  /**
   * Constructor.
   *
   * @param componentMetadata
   *          Metadata of the component container that the {@link MetaTypeProvider} belongs to.
   * @param bundleContext
   *          The context of the bundle that created the component container.
   */
  public MetatypeProviderImpl(final ComponentMetadata componentMetadata,
      final BundleContext bundleContext) {
    this.componentMetadata = componentMetadata;
    this.bundleContext = bundleContext;

    this.locales = createLocales();
  }

  private <T> T[] cloneIfNotNull(final T[] original) {
    if (original == null) {
      final T[] undefinedValue = null;
      return undefinedValue;
    }
    return original.clone();
  }

  private String[] createLocales() {
    String localizationBase = componentMetadata.getLocalizationBase();
    if (localizationBase == null) {
      return cloneIfNotNull(null);
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
      return cloneIfNotNull(null);
    }

    int localizationBaseLength = localizationBase.length();
    Set<String> result = new HashSet<String>();
    int propertiesExtensionLength = propertiesPattern.length() - 1;
    for (String resource : resources) {
      String locale = resource.substring(localizationBaseLength, resource.length()
          - propertiesExtensionLength);
      if (locale.length() == 0) {
        result.add("");
      } else if (locale.startsWith(LOCALE_SEPARATOR)) {
        result.add(locale.substring(1));
      }
    }

    if (result.size() == 0) {
      return cloneIfNotNull(null);
    }
    return result.toArray(new String[result.size()]);
  }

  @Override
  public String[] getLocales() {
    return cloneIfNotNull(locales);
  }

  @Override
  public ObjectClassDefinition getObjectClassDefinition(final String id,
      final String localeString) {
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
        String[] localeParts = localeString.split(LOCALE_SEPARATOR);
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
