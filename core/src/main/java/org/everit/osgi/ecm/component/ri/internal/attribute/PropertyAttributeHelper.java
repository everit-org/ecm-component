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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.everit.osgi.ecm.component.ConfigurationException;
import org.everit.osgi.ecm.component.PasswordHolder;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.component.ri.internal.ComponentContextImpl;
import org.everit.osgi.ecm.component.ri.internal.IllegalMetadataException;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.PasswordAttributeMetadata;
import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;
import org.osgi.framework.Constants;

/**
 * Helper class to manage attributes of components that have property behavior (setter methods).
 *
 * @param <C>
 *          The type of the component.
 * @param <V_ARRAY>
 *          The type of the property default value array.
 */
public class PropertyAttributeHelper<C, V_ARRAY> {

  private static final Map<Class<?>, Class<?>> PRIMITIVE_BOXING_TYPE_MAPPING;

  static {
    PRIMITIVE_BOXING_TYPE_MAPPING = new HashMap<>();
    PRIMITIVE_BOXING_TYPE_MAPPING.put(boolean.class, Boolean.class);
    PRIMITIVE_BOXING_TYPE_MAPPING.put(byte.class, Byte.class);
    PRIMITIVE_BOXING_TYPE_MAPPING.put(char.class, Character.class);
    PRIMITIVE_BOXING_TYPE_MAPPING.put(double.class, Double.class);
    PRIMITIVE_BOXING_TYPE_MAPPING.put(float.class, Float.class);
    PRIMITIVE_BOXING_TYPE_MAPPING.put(int.class, Integer.class);
    PRIMITIVE_BOXING_TYPE_MAPPING.put(long.class, Long.class);
    PRIMITIVE_BOXING_TYPE_MAPPING.put(short.class, Short.class);
  }

  private final PropertyAttributeMetadata<V_ARRAY> attributeMetadata;

  private final ComponentContextImpl<C> componentContext;

  private final MethodHandle methodHandle;

  private Class<?> parameterClass;

  /**
   * Constructor.
   *
   * @param componentContext
   *          Context of the component that the property belongs to.
   * @param attributeMetadata
   *          The metadata of the attribute.
   */
  public PropertyAttributeHelper(final ComponentContextImpl<C> componentContext,
      final PropertyAttributeMetadata<V_ARRAY> attributeMetadata) {

    this.componentContext = componentContext;
    this.attributeMetadata = attributeMetadata;
    this.methodHandle = resolveMethodHandle();

  }

  /**
   * Calls the setter of the property if it is available and the component is not failed.
   *
   * @param newValue
   *          The new value of the property that is passed to the setter.
   */
  public void applyValue(final Object newValue) {
    if (methodHandle == null) {
      return;
    }
    Object parameterValue = resolveValue(newValue);
    if (componentContext.getState() == ComponentState.FAILED) {
      return;
    }

    C instance = componentContext.getInstance();

    methodHandle.bindTo(instance);
    try {
      methodHandle.invoke(instance, parameterValue);
    } catch (Throwable e) {
      componentContext.fail(e, false);
    }

  }

  private void checkAttributeOptionality() {
    if (!attributeMetadata.isOptional()) {
      failDuringValueResolution("Mandatory attribute is not specified");
    }
  }

  private Object convertNonEmptyStringAttributeValueToChar(final String stringValue) {
    if (stringValue.length() == 1) {
      return stringValue.charAt(0);
    } else if (stringValue.length() > 1) {
      failDuringValueResolution(
          "String value with multiple characters cannot be converted to char type");
    }
    return null;
  }

  private void failDuringValueResolution(final String message) {
    Map<String, Object> properties = componentContext.getProperties();
    String servicePid = (String) properties.get(Constants.SERVICE_PID);
    ComponentMetadata componentMetadata = componentContext.getComponentContainer()
        .getComponentMetadata();
    if (servicePid == null) {
      servicePid = componentMetadata.getComponentId();
    }
    Throwable e = new ConfigurationException("Error during updating attribute '"
        + attributeMetadata.getAttributeId() + "' in configuration of component '"
        + servicePid + "' declared in class '" + componentMetadata.getType() + "': "
        + message);
    componentContext.fail(e, false);
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
    failDuringValueResolution(sb.toString());
  }

  public PropertyAttributeMetadata<V_ARRAY> getAttributeMetadata() {
    return attributeMetadata;
  }

  private MethodHandle resolveMethodHandle() {
    Method setter = resolveSetter();
    if (setter == null) {
      return null;
    }

    parameterClass = setter.getParameterTypes()[0];

    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      return lookup.unreflect(setter);
    } catch (IllegalAccessException e) {
      componentContext.fail(e, true);
    }

    return null;
  }

  private Object resolveMultiPasswordParamValue(final Object valueObject) {
    Class<?> componentType = valueObject.getClass().getComponentType();
    if (!PasswordHolder.class.equals(componentType) && !String.class.equals(componentType)) {
      failDuringValueResolution(
          "Either 'String[]' or 'PasswordHolder[]' was expected, but got '" + componentType
              + "[]'");
      return null;
    }

    if (componentType.equals(String.class)) {
      String[] stringPasswordArray = (String[]) valueObject;
      PasswordHolder[] result = new PasswordHolder[stringPasswordArray.length];
      for (int i = 0; i < stringPasswordArray.length; i++) {
        result[i] = new PasswordHolder(stringPasswordArray[i]);
      }
      return result;
    }

    PasswordHolder[] passwordHolderArray = (PasswordHolder[]) valueObject;
    String[] result = new String[passwordHolderArray.length];
    for (int i = 0; i < passwordHolderArray.length; i++) {
      result[i] = passwordHolderArray[i].getPassword();
    }
    return result;
  }

  private Object resolveNonExactTypeMatchMultiValue(final Object valueObject) {

    Class<? extends Object> valueClass = valueObject.getClass();

    if (!valueClass.isArray()) {
      failDuringValueResolution(
          "An array was expected as value , but got '" + valueClass.getCanonicalName() + '\'');
      return null;
    }

    if (!(attributeMetadata instanceof PasswordAttributeMetadata)) {
      failDuringValueResolution("'" + parameterClass.getCanonicalName()
          + "' was expected, but got '" + valueClass.getCanonicalName() + "'");
    }

    return resolveMultiPasswordParamValue(valueObject);
  }

  private Method resolveSetter() {
    MethodDescriptor setterMethodDescriptor = attributeMetadata.getSetter();
    if (setterMethodDescriptor == null) {
      return null;
    }
    Method method = setterMethodDescriptor.locate(componentContext.getComponentType(), false);

    Class<?>[] parameterTypes = method.getParameterTypes();
    if (parameterTypes.length != 1) {
      throwIllegalSetter(method,
          "Setter method must have one parameter: " + method.toGenericString());
      return null;
    }

    if (attributeMetadata.isMultiple()) {
      return resolveSetterForMultipleCardinalityAttribute(method, parameterTypes);
    } else {
      return resolveSetterForSimpleAttribute(method, parameterTypes);
    }
  }

  private Method resolveSetterForMultipleCardinalityAttribute(final Method method,
      final Class<?>[] parameterTypes) {
    if (!parameterTypes[0].isArray()) {
      throwIllegalSetter(method, "Parameter type should be an array");
      return null;
    }
    Class<?> componentType = parameterTypes[0].getComponentType();
    Class<?> expectedComponentType = attributeMetadata.getValueType();

    if (!expectedComponentType.equals(componentType)) {
      if (attributeMetadata instanceof PasswordAttributeMetadata) {
        if (!PasswordHolder.class.equals(componentType)) {
          throwIllegalSetter(method,
              "Parameter type should be either String[] or PasswordVaueHolder[].");
          return null;
        }
      } else {
        throwIllegalSetter(method, "Parameter array should have '" + expectedComponentType
            + "' component type.");
        return null;
      }
    }
    return method;
  }

  private Method resolveSetterForSimpleAttribute(final Method method,
      final Class<?>[] parameterTypes) {
    Class<?> valueType = attributeMetadata.getValueType();
    if (!valueType.equals(parameterTypes[0])) {
      if (valueType.isPrimitive()) {
        Class<?> boxingType = PRIMITIVE_BOXING_TYPE_MAPPING.get(valueType);
        if (!boxingType.equals(parameterTypes[0])) {
          throwIllegalSetter(
              method, " Parameter should be " + valueType.getCanonicalName() + " or "
                  + boxingType.getCanonicalName() + ": " + method.toGenericString());
          return null;
        }
      } else if (attributeMetadata instanceof PasswordAttributeMetadata) {
        if (!PasswordHolder.class.equals(parameterTypes[0])) {
          throwIllegalSetter(method,
              "Parameter type should be either String or PasswordVaueHolder");
        }
      } else {
        throwIllegalSetter(
            method, " Parameter type should be " + valueType.getCanonicalName() + ": "
                + method.toGenericString());
      }
    }
    return method;
  }

  private Object resolveSimplePasswordParamValue(final Object simpleValue,
      final Class<?> originalValueType) {
    Class<?> valueType = simpleValue.getClass();
    if (!String.class.equals(valueType) && !PasswordHolder.class.equals(valueType)) {
      failDuringValueResolution("Either 'String[]' or 'PasswordHolder[]' was expected, but got '"
          + originalValueType + "'");
      return null;
    }
    if (String.class.equals(valueType)) {
      return new PasswordHolder((String) simpleValue);
    }
    return ((PasswordHolder) simpleValue).getPassword();
  }

  private Object resolveSimpleValue(final Object valueObject) {
    Object simpleValue = valueObject;
    Class<?> valueClass = valueObject.getClass();

    // Array can be accepted if the length of the array is ok
    if (valueClass.isArray()) {

      int length = Array.getLength(valueObject);
      if (length == 0) {
        if (!attributeMetadata.isOptional()) {
          failDuringValueResolution(
              "Mandatory non-array attribute cannot be specified with an empty array.");
        }
        return null;
      }

      if (length > 1) {
        failDuringValueResolution("Simple value of attribute cannot be specified"
            + " with an array that has more than one elements");
      }
      simpleValue = Array.get(valueObject, 0);
    }

    if (simpleValue == null) {
      checkAttributeOptionality();
      return null;
    }

    Class<?> simpleValueClass = simpleValue.getClass();

    if (simpleValueClass.equals(parameterClass)) {
      return simpleValue;
    }

    if (attributeMetadata instanceof PasswordAttributeMetadata) {
      return resolveSimplePasswordParamValue(simpleValue, valueClass);
    }

    if (parameterClass.equals(PRIMITIVE_BOXING_TYPE_MAPPING.get(simpleValueClass))
        || simpleValueClass.equals(PRIMITIVE_BOXING_TYPE_MAPPING.get(parameterClass))) {
      return simpleValue;
    }

    return tryConvertingSimpleValue(simpleValue);
  }

  private Object resolveValue(final Object valueObject) {
    // Handle null value
    if (valueObject == null) {
      checkAttributeOptionality();
      return null;
    }

    if (parameterClass.equals(valueObject.getClass())) {
      return valueObject;
    }

    if (attributeMetadata.isMultiple()) {
      return resolveNonExactTypeMatchMultiValue(valueObject);
    } else {
      return resolveSimpleValue(valueObject);
    }
  }

  private void throwIllegalSetter(final Method method, final String additionalMessage) {
    IllegalMetadataException e = new IllegalMetadataException("Invalid setter '"
        + method.toGenericString()
        + "' defined for attribute '" + attributeMetadata.getAttributeId() + "' of component '"
        + componentContext.getComponentContainer().getComponentMetadata().getComponentId() + "'. "
        + additionalMessage);

    componentContext.fail(e, true);
  }

  private Object tryConvertingSimpleValue(final Object simpleValue) {
    Class<? extends Object> originalValueType = simpleValue.getClass();
    Class<?> attributeType = attributeMetadata.getValueType();

    if (simpleValue instanceof String) {
      return tryConvertingStringValueToAttributeType(simpleValue);
    }

    failOnIncomaptibleSimpleType(originalValueType, attributeType);
    return null;
  }

  private Object tryConvertingStringValueToAttributeType(final Object simpleValue) {
    String stringValue = (String) simpleValue;

    Class<?> attributeType = attributeMetadata.getValueType();
    if (char.class.equals(attributeType)) {
      return convertNonEmptyStringAttributeValueToChar(stringValue);
    }

    if ("".equals(stringValue.trim())) {
      checkAttributeOptionality();
      return null;
    }

    if (boolean.class.equals(attributeType)) {
      return Boolean.valueOf(stringValue);
    }

    return tryConvertingStringValueToNumberOrFail(stringValue, attributeType);
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
              + attributeType.getCanonicalName() + "'");
    }

    failOnIncomaptibleSimpleType(String.class, attributeType);
    return null;
  }

}
