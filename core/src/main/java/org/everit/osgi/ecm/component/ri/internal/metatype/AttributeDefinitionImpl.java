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

import java.lang.reflect.Array;

import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.AttributeMetadataHolder;
import org.everit.osgi.ecm.metadata.PasswordAttributeMetadata;
import org.everit.osgi.ecm.metadata.SelectablePropertyAttributeMetadata;
import org.osgi.service.metatype.AttributeDefinition;

public class AttributeDefinitionImpl<V_ARRAY> implements AttributeDefinition,
    AttributeMetadataHolder<V_ARRAY> {

  private final AttributeMetadata<V_ARRAY> attributeMetadata;

  private final int attributeType;

  private final String[] defaultValue;

  private final Localizer localizer;

  private final String[] optionLabels;

  private final String[] optionValues;

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
      return null;
    }
    int length = Array.getLength(tmpDefaultValue);
    if (length == 0) {
      return null;
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
    return defaultValue;
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
    return optionLabels;
  }

  @Override
  public String[] getOptionValues() {
    return optionValues;
  }

  @Override
  public int getType() {
    return attributeType;
  }

  @Override
  public String validate(final String value) {
    // TODO Auto-generated method stub
    return null;
  }

}
