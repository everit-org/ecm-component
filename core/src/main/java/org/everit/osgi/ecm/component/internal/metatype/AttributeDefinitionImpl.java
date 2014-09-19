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

import org.everit.osgi.ecm.component.internal.Localizer;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.PasswordAttributeMetadata;
import org.osgi.service.metatype.AttributeDefinition;

public class AttributeDefinitionImpl implements AttributeDefinition {

    private final AttributeMetadata<?> attributeMetadata;

    private final int attributeType;

    private final Localizer localizer;

    public AttributeDefinitionImpl(AttributeMetadata<?> attributeMetadata, Localizer localizer) {
        this.attributeMetadata = attributeMetadata;
        this.localizer = localizer;

        Class<?> valueType = attributeMetadata.getValueType();

        if (valueType.equals(Boolean.class)) {
            attributeType = AttributeDefinition.BOOLEAN;
        } else if (valueType.equals(Byte.class)) {
            attributeType = AttributeDefinition.BYTE;
        } else if (valueType.equals(Character.class)) {
            attributeType = AttributeDefinition.CHARACTER;
        } else if (valueType.equals(Double.class)) {
            attributeType = AttributeDefinition.DOUBLE;
        } else if (valueType.equals(Float.class)) {
            attributeType = AttributeDefinition.FLOAT;
        } else if (valueType.equals(Integer.class)) {
            attributeType = AttributeDefinition.INTEGER;
        } else if (valueType.equals(Long.class)) {
            attributeType = AttributeDefinition.LONG;
        } else if (valueType.equals(Short.class)) {
            attributeType = AttributeDefinition.SHORT;
        } else if (attributeMetadata.getClass().equals(PasswordAttributeMetadata.class)) {
            attributeType = AttributeDefinition.PASSWORD;
        } else {
            attributeType = AttributeDefinition.STRING;
        }
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
        Object[] defaultValue = attributeMetadata.getDefaultValue();
        if (defaultValue == null || defaultValue.length == 0) {
            return null;
        }

        String[] result = new String[defaultValue.length];
        for (int i = 0; i < result.length; i++) {
            if (defaultValue[i] != null) {
                defaultValue[i] = String.valueOf(defaultValue[i]);
            }
        }
        return result;
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
    public String getName() {
        return localizer.localize(attributeMetadata.getLabel());
    }

    @Override
    public String[] getOptionLabels() {
        // TODO
        return null;
    }

    @Override
    public String[] getOptionValues() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getType() {
        return attributeType;
    }

    @Override
    public String validate(String value) {
        // TODO Auto-generated method stub
        return null;
    }

}
