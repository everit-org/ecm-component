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
package org.everit.osgi.ecm.component.ri.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.MetadataValidationException;
import org.everit.osgi.ecm.util.method.MethodDescriptor;
import org.everit.osgi.ecm.util.method.MethodUtil;
import org.osgi.framework.BundleContext;

/**
 * Helper class to resolve and call the activate method of the component. Resolving will similar
 * rules as it is defined in Declarative Services specification:
 *
 * <p>
 * If the metadata information contains parameter types for the activate method, the specified
 * method will be used. If parameter types are not provided, then the following methods will be
 * searched and the first result will be used:
 * <ul>
 * <li>a method with a {@link ComponentContext} parameter</li>
 * <li>a method with a {@link BundleContext} parameter</li>
 * <li>a method with a {@link Map} parameter</li>
 * <li>a method that contains one or more of the following parameters: {@link ComponentContext},
 * {@link BundleContext}, {@link Map}</li>
 * <li>a method without any parameter</li>
 * </ul>
 *
 * @param <C>
 *          Type of the Component implementation.
 */
public class ActivateMethodHelper<C> {

  private static final int MAX_ACTIVATE_METHOD_PARAM_NUM = 3;

  private final ComponentContextImpl<C> componentContext;

  private int indexOfBundleContextParameter = -1;

  private int indexOfComponentContextParameter = -1;

  private int indexOfPropertiesParameter = -1;

  private Method method = null;

  /**
   * Constructor.
   *
   * @param componentContext
   *          The ComponentContext.
   */
  public ActivateMethodHelper(final ComponentContextImpl<C> componentContext) {
    this.componentContext = componentContext;
    Class<C> componentType = componentContext.getComponentType();
    ComponentMetadata componentMetadata = componentContext.getComponentContainer()
        .getComponentMetadata();
    MethodDescriptor methodDescriptor = componentMetadata.getActivate();
    if (methodDescriptor == null) {
      return;
    }

    String methodName = methodDescriptor.getMethodName();
    Method locatedMethod = null;
    if (methodDescriptor.getParameterTypeNames() != null) {
      locatedMethod = methodDescriptor.locate(componentType, false);
      if (!validateMethod(locatedMethod)) {
        Exception e = new IllegalMetadataException("Invalid activate method: "
            + locatedMethod.toGenericString());
        componentContext.fail(e, true);
        return;
      }
    } else {
      locatedMethod = new MethodDescriptor(methodName,
          new String[] { ComponentContext.class.getName() }).locate(componentType, false);

      if (locatedMethod == null) {
        locatedMethod = new MethodDescriptor(methodName,
            new String[] { BundleContext.class.getName() }).locate(componentType, false);
      }

      if (locatedMethod == null) {
        locatedMethod = new MethodDescriptor(methodName, new String[] { Map.class.getName() })
            .locate(componentType, false);
      }

      if (locatedMethod == null) {
        locatedMethod = locateMethodWithMinTwoParams(componentType);
      }

      if (componentContext.isFailed()) {
        return;
      }

      if (locatedMethod == null) {
        locatedMethod = new MethodDescriptor(methodName, new String[0])
            .locate(componentType, false);
      }
    }
    if (locatedMethod == null) {
      Exception e = new MetadataValidationException(
          "Could not find activate method for component '"
              + componentMetadata.getComponentId() + " based on descriptor: "
              + methodDescriptor.toString());

      componentContext.fail(e, true);

      return;
    }

    method = locatedMethod;
    initializeParameterIndexes();

  }

  /**
   * Call the activate method on the component instance.
   *
   * @param instance
   *          The component instance.
   * @throws IllegalAccessException
   *           thrown by java reflection API.
   * @throws InvocationTargetException
   *           if activate method throws an exception.
   */
  public void call(final Object instance)
      throws IllegalAccessException, InvocationTargetException {
    if (method == null) {
      return;
    }

    int paramNum = method.getParameterTypes().length;
    Object[] parameters = new Object[paramNum];

    if (paramNum > 0) {
      if (indexOfBundleContextParameter >= 0) {
        parameters[indexOfBundleContextParameter] = componentContext.getBundleContext();
      }
      if (indexOfComponentContextParameter >= 0) {
        parameters[indexOfComponentContextParameter] = componentContext;
      }
      if (indexOfPropertiesParameter >= 0) {
        parameters[indexOfPropertiesParameter] = componentContext.getProperties();
      }
    }

    method.invoke(instance, parameters);
  }

  private void initializeParameterIndexes() {
    Class<?>[] parameterTypes = method.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      Class<?> parameterType = parameterTypes[i];
      if (parameterType.equals(ComponentContext.class)) {
        indexOfComponentContextParameter = i;
      } else if (parameterType.equals(BundleContext.class)) {
        indexOfBundleContextParameter = i;
      } else if (Map.class.isAssignableFrom(parameterType)) {
        indexOfPropertiesParameter = i;
      } else {
        componentContext.fail(new MetadataValidationException(
            "Unrecognized type in Activate method: "
                + parameterType),
            true);
      }
    }
  }

  private Method locateMethodWithMinTwoParams(final Class<?> clazz) {
    Class<?> currentClass = clazz;

    Method foundMethod = null;
    while ((currentClass != null) && (foundMethod == null)) {
      Method[] declaredMethods;
      try {
        declaredMethods = currentClass.getDeclaredMethods();
      } catch (NoClassDefFoundError e) {
        componentContext.fail(e, true);
        return null;
      }

      for (int i = 0; (i < declaredMethods.length) && (foundMethod == null); i++) {
        Method declaredMethod = declaredMethods[i];
        Class<?>[] parameterTypes = method.getParameterTypes();
        int parameterNum = parameterTypes.length;
        if (MethodUtil.isMethodAccessibleFromClass(clazz, declaredMethod, false)
            && (parameterNum >= 2) && (parameterNum <= MAX_ACTIVATE_METHOD_PARAM_NUM)
            && validateMethod(declaredMethod)) {

          foundMethod = declaredMethod;
        }
      }

      currentClass = currentClass.getSuperclass();
    }
    return foundMethod;
  }

  private boolean validateMethod(final Method pMethod) {
    Class<?>[] parameterTypes = pMethod.getParameterTypes();
    if (parameterTypes.length > MAX_ACTIVATE_METHOD_PARAM_NUM) {
      return false;
    }

    Set<Class<?>> collectedParameterTypes = new HashSet<>();
    for (Class<?> parameterType : parameterTypes) {
      if (!parameterType.equals(ComponentContext.class) && !parameterType.equals(Map.class)
          && !parameterType.equals(BundleContext.class)) {

        return false;
      }
      boolean added = collectedParameterTypes.add(parameterType);
      if (!added) {
        return false;
      }
    }
    return true;
  }
}
