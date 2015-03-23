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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.AttributeMetadataHolder;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.Icon;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Implementation of {@link ObjectClassDefinition} for ECM based component containers.
 *
 * @param <C>
 *          Type of the component implementation.
 */
public class ObjectClassDefinitionImpl<C> implements ObjectClassDefinition {

  private final AttributeDefinition[] attributeDefinitions;

  private final ClassLoader classLoader;

  private final ComponentMetadata componentMetadata;

  private final Localizer localizer;

  /**
   * Constructor.
   */
  public ObjectClassDefinitionImpl(final ComponentMetadata componentMetadata,
      final Localizer localizer, final ClassLoader classLoader) {

    this.componentMetadata = componentMetadata;
    this.localizer = localizer;
    this.classLoader = classLoader;

    attributeDefinitions = createAttributeDefinitions(componentMetadata);

  }

  private AttributeDefinition[] createAttributeDefinitions(
      final ComponentMetadata pComponentMetadata) {

    AttributeMetadata<?>[] attributes = pComponentMetadata.getAttributes();
    if ((attributes == null) || (attributes.length == 0)) {
      return noAttributeDefinitions();
    }

    List<AttributeDefinition> result = new LinkedList<AttributeDefinition>();

    for (AttributeMetadata<?> attribute : attributes) {
      @SuppressWarnings({ "unchecked", "rawtypes" })
      AttributeDefinitionImpl<Object> attributeDefinition = new AttributeDefinitionImpl(attribute,
          localizer);

      result.add(attributeDefinition);
    }

    if (result.size() == 0) {
      return noAttributeDefinitions();
    }

    return result.toArray(new AttributeDefinition[result.size()]);
  }

  private AttributeDefinition[] generateAttributeDefinitions(final boolean required) {
    List<AttributeDefinition> result = new ArrayList<AttributeDefinition>();
    for (AttributeDefinition ad : attributeDefinitions) {
      AttributeMetadataHolder<?> amh = (AttributeMetadataHolder<?>) ad;
      AttributeMetadata<?> attributeMetadata = amh.getMetadata();
      if ((required && !attributeMetadata.isOptional())
          || (!required && attributeMetadata.isOptional())) {
        result.add(ad);
      }
    }
    if (result.size() == 0) {
      return noAttributeDefinitions();
    }
    return result.toArray(new AttributeDefinition[result.size()]);
  }

  @Override
  public AttributeDefinition[] getAttributeDefinitions(final int filter) {
    if (attributeDefinitions == null) {
      return returnNull();
    }

    if (filter == ObjectClassDefinition.REQUIRED) {
      return generateAttributeDefinitions(true);
    } else if (filter == ObjectClassDefinition.OPTIONAL) {
      return generateAttributeDefinitions(false);
    } else {
      return attributeDefinitions.clone();
    }
  }

  @Override
  public String getDescription() {
    return localizer.localize(componentMetadata.getDescription());
  }

  @Override
  public InputStream getIcon(final int size) throws IOException {
    Icon[] icons = componentMetadata.getIcons();
    if ((icons == null) || (icons.length == 0)) {
      return null;
    }

    int difference = Integer.MAX_VALUE;
    Icon selectedIcon = null;
    for (Icon icon : icons) {
      int currentDifference = Math.abs(size - icon.getSize());
      if (currentDifference < difference) {
        selectedIcon = icon;
      }
    }

    String iconPath = localizer.localize(selectedIcon.getPath());
    if (iconPath != null) {
      classLoader.getResourceAsStream(iconPath);
      return null;
    } else {
      return null;
    }
  }

  @Override
  public String getID() {
    return componentMetadata.getConfigurationPid();
  }

  @Override
  public String getName() {
    return localizer.localize(componentMetadata.getLabel());
  }

  private AttributeDefinition[] noAttributeDefinitions() {
    final AttributeDefinition[] noAttributeDefinitions = null;
    return noAttributeDefinitions;
  }

  private <N> N returnNull() {
    return null;
  }

}
