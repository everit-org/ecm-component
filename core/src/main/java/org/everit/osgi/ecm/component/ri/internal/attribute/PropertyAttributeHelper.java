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
import java.lang.reflect.Method;

import org.everit.osgi.ecm.component.PasswordHolder;
import org.everit.osgi.ecm.component.ri.internal.ComponentContextImpl;
import org.everit.osgi.ecm.component.ri.internal.IllegalMetadataException;
import org.everit.osgi.ecm.metadata.PasswordAttributeMetadata;
import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;

/**
 * Helper class to manage attributes of components that have property behavior (setter methods).
 *
 * @param <C>
 *          The type of the component.
 * @param <V_ARRAY>
 *          The type of the property default value array.
 */
public class PropertyAttributeHelper<C, V_ARRAY> {

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

    Object parameterValue;
    try {
      parameterValue = resolveValue(newValue);
    } catch (RuntimeException e) {
      componentContext.fail(e, false);
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
      PropertyAttributeUtil.failDuringValueResolution(
          "Either 'String[]' or 'PasswordHolder[]' was expected, but got '" + componentType
              + "[]'",
          componentContext, attributeMetadata);
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
      PropertyAttributeUtil.failDuringValueResolution(
          "An array was expected as value , but got '" + valueClass.getCanonicalName() + '\'',
          componentContext, attributeMetadata);
      return null;
    }

    if (!(attributeMetadata instanceof PasswordAttributeMetadata)) {
      PropertyAttributeUtil.failDuringValueResolution("'" + parameterClass.getCanonicalName()
          + "' was expected, but got '" + valueClass.getCanonicalName() + "'",
          componentContext, attributeMetadata);
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
        Class<?> boxingType = PropertyAttributeUtil.PRIMITIVE_BOXING_TYPE_MAPPING.get(valueType);
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
      PropertyAttributeUtil.failDuringValueResolution(
          "Either 'String[]' or 'PasswordHolder[]' was expected, but got '"
              + originalValueType + "'",
          componentContext, attributeMetadata);
      return null;
    }
    if (String.class.equals(valueType)) {
      return new PasswordHolder((String) simpleValue);
    }
    return ((PasswordHolder) simpleValue).getPassword();
  }

  private Object resolveSimpleValue(final Object valueObject) {
    Object simpleValue = PropertyAttributeUtil.resolveSimpleValueEvenIfItIsInOneElementArray(
        valueObject, componentContext, attributeMetadata);
    if (componentContext.isFailed()) {
      return null;
    }

    Class<?> simpleValueClass = simpleValue.getClass();

    if (simpleValueClass.equals(parameterClass)) {
      return simpleValue;
    }

    if (attributeMetadata instanceof PasswordAttributeMetadata) {
      return resolveSimplePasswordParamValue(simpleValue, valueObject.getClass());
    }

    if (PropertyAttributeUtil.typesEqualWithOrWithoutBoxing(parameterClass, simpleValueClass)) {
      return simpleValue;
    }

    return PropertyAttributeUtil.tryConvertingSimpleValue(simpleValue, parameterClass,
        componentContext, attributeMetadata);
  }

  private Object resolveValue(final Object valueObject) {
    // Handle null value
    if (valueObject == null) {
      PropertyAttributeUtil.checkAttributeOptional(componentContext, attributeMetadata);
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

}
