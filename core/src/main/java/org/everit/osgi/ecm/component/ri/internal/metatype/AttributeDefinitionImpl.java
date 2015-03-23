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

import java.lang.reflect.Array;

import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.AttributeMetadataHolder;
import org.everit.osgi.ecm.metadata.PasswordAttributeMetadata;
import org.everit.osgi.ecm.metadata.SelectablePropertyAttributeMetadata;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Implemenation class of {@link AttributeDefinition} that provides information about an attribute
 * of the component via MetatypeService.
 *
 * @param <V_ARRAY>
 *          The type of the default value array of the attribute.
 */
public class AttributeDefinitionImpl<V_ARRAY> implements AttributeDefinition,
    AttributeMetadataHolder<V_ARRAY> {

  private final AttributeMetadata<V_ARRAY> attributeMetadata;

  private final int attributeType;

  private final String[] defaultValue;

  private final Localizer localizer;

  private final String[] optionLabels;

  private final String[] optionValues;

  /**
   * Constructor.
   *
   * @param attributeMetadata
   *          The metadata information of the component attribute.
   * @param localizer
   *          The localizer helps generating localized representation of the labels.
   */
  public AttributeDefinitionImpl(final AttributeMetadata<V_ARRAY> attributeMetadata,
      final Localizer localizer) {
    this.attributeMetadata = attributeMetadata;
    this.localizer = localizer;

    Class<?> valueType = attributeMetadata.getValueType();

    attributeType = convertValueTypeToAttributeType(valueType);

    defaultValue = createDefaultValueArray();

    if (attributeMetadata instanceof SelectablePropertyAttributeMetadata) {
      SelectablePropertyAttributeMetadata<V_ARRAY> selectableMetadata =
          (SelectablePropertyAttributeMetadata<V_ARRAY>) attributeMetadata;

      String[] tmpOptionLabels = selectableMetadata.getOptionLabels();

      V_ARRAY tmpOptionValues = selectableMetadata.getOptionValues();
      if (tmpOptionValues == null) {
        optionValues = null;
        optionLabels = null;
      } else {
        int length = Array.getLength(tmpOptionValues);
        if (tmpOptionLabels == null) {
          optionLabels = null;
        } else {
          optionLabels = new String[length];
        }

        optionValues = new String[length];
        for (int i = 0; i < length; i++) {
          Object optionValue = Array.get(tmpOptionValues, i);
          if (optionValue != null) {
            optionValues[i] = String.valueOf(optionValue);
          }
          if (optionLabels != null) {
            if (tmpOptionLabels[i] != null) {
              optionLabels[i] = localizer.localize(tmpOptionLabels[i]);
            } else {
              optionLabels[i] = optionValues[i];
            }
          }
        }
      }
    } else {
      optionLabels = null;
      optionValues = null;
    }
  }

  private <T> T[] cloneIfNotNull(final T[] original) {
    if (original == null) {
      final T[] undefinedValue = null;
      return undefinedValue;
    }
    return original.clone();
  }

  private int convertValueTypeToAttributeType(final Class<?> valueType) {
    if (boolean.class.equals(valueType)) {
      return AttributeDefinition.BOOLEAN;
    } else if (byte.class.equals(valueType)) {
      return AttributeDefinition.BYTE;
    } else if (char.class.equals(valueType)) {
      return AttributeDefinition.CHARACTER;
    } else if (double.class.equals(valueType)) {
      return AttributeDefinition.DOUBLE;
    } else if (float.class.equals(valueType)) {
      return AttributeDefinition.FLOAT;
    } else if (int.class.equals(valueType)) {
      return AttributeDefinition.INTEGER;
    } else if (long.class.equals(valueType)) {
      return AttributeDefinition.LONG;
    } else if (short.class.equals(valueType)) {
      return AttributeDefinition.SHORT;
    } else if (attributeMetadata.getClass().equals(PasswordAttributeMetadata.class)) {
      return AttributeDefinition.PASSWORD;
    } else {
      return AttributeDefinition.STRING;
    }
  }

  private String[] createDefaultValueArray() {
    V_ARRAY tmpDefaultValue = attributeMetadata.getDefaultValue();
    if (tmpDefaultValue == null) {
      return cloneIfNotNull(null);
    }
    int length = Array.getLength(tmpDefaultValue);
    if (length == 0) {
      return cloneIfNotNull(null);
    }

    String[] result = new String[length];
    for (int i = 0; i < result.length; i++) {
      Object element = Array.get(tmpDefaultValue, i);
      if (element != null) {
        result[i] = String.valueOf(element);
      }
    }
    return result;
  }

  @Override
  public int getCardinality() {
    if (attributeMetadata.isMultiple()) {
      return Integer.MAX_VALUE;
    } else {
      return 0;
    }
  }

  @Override
  public String[] getDefaultValue() {
    return cloneIfNotNull(defaultValue);
  }

  @Override
  public String getDescription() {
    return localizer.localize(attributeMetadata.getDescription());
  }

  @Override
  public String getID() {
    return attributeMetadata.getAttributeId();
  }

  @Override
  public AttributeMetadata<V_ARRAY> getMetadata() {
    return attributeMetadata;
  }

  @Override
  public String getName() {
    return localizer.localize(attributeMetadata.getLabel());
  }

  @Override
  public String[] getOptionLabels() {
    return cloneIfNotNull(optionLabels);
  }

  @Override
  public String[] getOptionValues() {
    return cloneIfNotNull(optionValues);
  }

  @Override
  public int getType() {
    return attributeType;
  }

  @Override
  public String validate(final String value) {
    return null;
  }

}
