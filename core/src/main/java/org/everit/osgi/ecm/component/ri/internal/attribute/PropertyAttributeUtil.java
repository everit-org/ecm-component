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
package org.everit.osgi.ecm.component.ri.internal.attribute;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.everit.osgi.ecm.component.ConfigurationException;
import org.everit.osgi.ecm.component.PasswordHolder;
import org.everit.osgi.ecm.component.ri.internal.ComponentContextImpl;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.Constants;

/**
 * Utility methods to process component attributes.
 */
public final class PropertyAttributeUtil {

  public static final Map<Class<?>, Class<?>> PRIMITIVE_BOXING_TYPE_MAPPING;

  static {
    Map<Class<?>, Class<?>> primitiveBoxingTypeMapping = new HashMap<>();
    primitiveBoxingTypeMapping.put(boolean.class, Boolean.class);
    primitiveBoxingTypeMapping.put(byte.class, Byte.class);
    primitiveBoxingTypeMapping.put(char.class, Character.class);
    primitiveBoxingTypeMapping.put(double.class, Double.class);
    primitiveBoxingTypeMapping.put(float.class, Float.class);
    primitiveBoxingTypeMapping.put(int.class, Integer.class);
    primitiveBoxingTypeMapping.put(long.class, Long.class);
    primitiveBoxingTypeMapping.put(short.class, Short.class);
    PRIMITIVE_BOXING_TYPE_MAPPING = Collections.unmodifiableMap(primitiveBoxingTypeMapping);
  }

  /**
   * Checks if the attribute is optional and if not, fails the component.
   *
   * @param componentContextImpl
   *          The component context.
   * @param attributeMetadata
   *          The metadata of the attribute to check.
   */
  public static void checkAttributeOptional(final ComponentContextImpl<?> componentContextImpl,
      final AttributeMetadata<?> attributeMetadata) {
    if (!attributeMetadata.isOptional()) {
      failDuringValueResolution("Mandatory attribute is not specified", componentContextImpl,
          attributeMetadata);
    }
  }

  /**
   * Sets the component to be failed due to attribute value resolution. The message of the error is
   * extended with the service pid or component id and the attribute id.
   *
   * @param message
   *          The message of the failure.
   * @param componentContext
   *          The context of the component.
   * @param attributeMetadata
   *          The metadata of the attribute.
   */
  public static void failDuringValueResolution(final String message,
      final ComponentContextImpl<?> componentContext,
      final AttributeMetadata<?> attributeMetadata) {

    Map<String, Object> properties = componentContext.getProperties();
    String servicePid = (String) properties.get(Constants.SERVICE_PID);
    ComponentMetadata componentMetadata = componentContext.getComponentContainer()
        .getComponentMetadata();
    if (servicePid == null) {
      servicePid = componentMetadata.getComponentId();
    }
    ConfigurationException e = new ConfigurationException("Error during updating attribute '"
        + attributeMetadata.getAttributeId() + "' in configuration of component '"
        + servicePid + "' declared in class '" + componentMetadata.getType() + "': "
        + message);

    throw e;
  }

  /**
   * Resolves the simple value from a value object even if the value object is a one element array.
   * If resolving is not possible, sets the component state to failed.
   *
   * @param valueObject
   *          The original value.
   * @param componentContext
   *          The context of the component.
   * @param attributeMetadata
   *          The attribute metadata.
   * @return The resolved simple value that can be null if the attribute is optional.
   */
  public static Object resolveSimpleValueEvenIfItIsInOneElementArray(final Object valueObject,
      final ComponentContextImpl<?> componentContext,
      final AttributeMetadata<?> attributeMetadata) {

    if (valueObject == null) {
      PropertyAttributeUtil.checkAttributeOptional(componentContext, attributeMetadata);
    }

    Object simpleValue = valueObject;
    Class<?> valueClass = valueObject.getClass();

    // Array can be accepted if the length of the array is ok
    if (valueClass.isArray()) {

      int length = Array.getLength(valueObject);
      if (length == 0) {
        if (!attributeMetadata.isOptional()) {
          PropertyAttributeUtil.failDuringValueResolution(
              "Mandatory non-array attribute cannot be specified with an empty array.",
              componentContext, attributeMetadata);
        }
        return null;
      } else if (length > 1) {
        PropertyAttributeUtil
            .failDuringValueResolution("Simple value of attribute cannot be specified"
                + " with an array that has more than one elements", componentContext,
                attributeMetadata);

        return null;
      }
      simpleValue = Array.get(valueObject, 0);
    }

    if (simpleValue == null) {
      PropertyAttributeUtil.checkAttributeOptional(componentContext, attributeMetadata);
    }
    return simpleValue;
  }

  /**
   * Trying to convert a non-multiple attribute value to a target type. Currently from string to any
   * other supported type conversion is supported. After this method it is necessary to check the
   * component state as it might become failed.
   *
   * @param simpleValue
   *          The current value of the non-multiple attribute.
   * @param targetType
   *          The type to convert to.
   * @param componentContext
   *          The context of the component.
   * @param attributeMetadata
   *          The metadata of the attribute.
   * @return The converted value.
   */
  public static Object tryConvertingSimpleValue(final Object simpleValue,
      final Class<?> targetType, final ComponentContextImpl<?> componentContext,
      final AttributeMetadata<?> attributeMetadata) {

    Class<? extends Object> originalValueType = simpleValue.getClass();

    if (originalValueType.equals(targetType)) {
      return simpleValue;
    }

    PropertyAttributeUtil instance = new PropertyAttributeUtil(componentContext, attributeMetadata);
    if (simpleValue instanceof String) {
      return instance.tryConvertingStringValueToAttributeType(simpleValue, targetType);
    } else if (simpleValue instanceof PasswordHolder && targetType.equals(String.class)) {
      return ((PasswordHolder) simpleValue).getPassword();
    }

    instance.failOnIncomaptibleSimpleType(originalValueType, targetType);

    return null;
  }

  /**
   * Checks whether the two types are equal or they can be equal by using primitive to non-primitive
   * boxing.
   *
   * @param type1
   *          Type one.
   * @param type2
   *          Type two.
   * @return Whether the types are equal or one of them are primitive and the other the class
   *         representation of the primitive type.
   */
  public static boolean typesEqualWithOrWithoutBoxing(final Class<? extends Object> type1,
      final Class<?> type2) {

    return type1.equals(type2) || type2
        .equals(
            PropertyAttributeUtil.PRIMITIVE_BOXING_TYPE_MAPPING.get(type1))
        || type1
            .equals(PropertyAttributeUtil.PRIMITIVE_BOXING_TYPE_MAPPING.get(type2));
  }

  private final AttributeMetadata<?> attributeMetadata;

  private final ComponentContextImpl<?> componentContext;

  private PropertyAttributeUtil(final ComponentContextImpl<?> componentContextImpl,
      final AttributeMetadata<?> attributeMetadata) {
    componentContext = componentContextImpl;
    this.attributeMetadata = attributeMetadata;
  }

  private Object convertNonEmptyStringAttributeValueToChar(final String stringValue) {
    if (stringValue.length() == 1) {
      return stringValue.charAt(0);
    } else if (stringValue.length() > 1) {
      failDuringValueResolution(
          "String value with multiple characters cannot be converted to char type",
          componentContext, attributeMetadata);
    }
    return null;
  }

  private void failOnIncomaptibleSimpleType(final Class<? extends Object> valueType,
      final Class<?> attributeType) {

    StringBuilder sb = new StringBuilder();
    if (attributeType.isPrimitive()) {
      sb.append("Either ")
          .append(PRIMITIVE_BOXING_TYPE_MAPPING.get(attributeType).getCanonicalName())
          .append(" or ");
    }
    sb.append(attributeType.getCanonicalName()).append(" was expected, but got ")
        .append(valueType.getCanonicalName());
    failDuringValueResolution(sb.toString(), componentContext, attributeMetadata);
  }

  private Object tryConvertingStringValueToAttributeType(final Object simpleValue,
      final Class<?> targetType) {
    String stringValue = (String) simpleValue;

    if (char.class.equals(targetType)) {
      return convertNonEmptyStringAttributeValueToChar(stringValue);
    }

    if ("".equals(stringValue.trim())) {
      checkAttributeOptional(componentContext, attributeMetadata);
      return null;
    }

    if (boolean.class.equals(targetType)) {
      return Boolean.valueOf(stringValue);
    }

    return tryConvertingStringValueToNumberOrFail(stringValue, targetType);
  }

  private Object tryConvertingStringValueToNumberOrFail(final String stringValue,
      final Class<?> attributeType) {

    try {
      if (byte.class.equals(attributeType)) {
        return Byte.valueOf(stringValue);
      } else if (double.class.equals(attributeType)) {
        return Double.valueOf(stringValue);
      } else if (float.class.equals(attributeType)) {
        return Float.valueOf(stringValue);
      } else if (int.class.equals(attributeType)) {
        return Integer.valueOf(stringValue);
      } else if (long.class.equals(attributeType)) {
        return Long.valueOf(stringValue);
      } else if (short.class.equals(attributeType)) {
        return Short.valueOf(stringValue);
      }
    } catch (NumberFormatException e) {
      failDuringValueResolution(
          "String value \"" + stringValue + "\" cannot be converted to type '"
              + attributeType.getCanonicalName() + "'",
          componentContext, attributeMetadata);
    }

    failOnIncomaptibleSimpleType(String.class, attributeType);
    return null;
  }
}
