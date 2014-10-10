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
package org.everit.osgi.ecm.component.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.everit.osgi.ecm.component.context.ComponentContext;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;
import org.everit.osgi.ecm.util.method.MethodUtil;
import org.osgi.framework.BundleContext;

public class ActivateMethodHelper<C> {

    private int indexOfBundleContextParameter = -1;

    private int indexOfComponentContextParameter = -1;

    private int indexOfPropertiesParameter = -1;

    private Method method = null;

    public ActivateMethodHelper(ComponentMetadata componentMetadata, Class<?> clazz) {
        MethodDescriptor methodDescriptor = componentMetadata.getActivate();
        if (methodDescriptor == null) {
            return;
        }

        String methodName = methodDescriptor.getMethodName();
        Method locatedMethod = null;
        if (methodDescriptor.getParameterTypeNames() != null) {
            locatedMethod = methodDescriptor.locate(clazz, false);
            validateMethod(locatedMethod);
        } else {
            locatedMethod = new MethodDescriptor(methodName, new String[] { ComponentContext.class.getName() })
                    .locate(clazz, false);

            if (locatedMethod == null) {
                locatedMethod = new MethodDescriptor(methodName, new String[] { BundleContext.class.getName() })
                        .locate(clazz, false);
            }

            if (locatedMethod == null) {
                locatedMethod = new MethodDescriptor(methodName, new String[] { Map.class.getName() })
                        .locate(clazz, false);
            }

            if (locatedMethod == null) {
                locatedMethod = locateMethodWithMinTwoParams(clazz);
            }

            if (locatedMethod == null) {
                locatedMethod = new MethodDescriptor(methodName, new String[0]).locate(clazz, false);
            }
        }
        if (locatedMethod == null) {
            throw new IllegalMetadataException("Could not find activate method for component '"
                    + componentMetadata.getComponentId() + " based on descriptor: " + methodDescriptor.toString());
        }

        method = locatedMethod;
        initializeParameterIndexes();

    }

    public void call(ComponentContext<C> componentContext, Object instance) throws IllegalAccessException,
            InvocationTargetException {
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
            } else {
                indexOfPropertiesParameter = i;
            }
        }
    }

    private Method locateMethodWithMinTwoParams(Class<?> clazz) {
        Class<?> currentClass = clazz;

        Method foundMethod = null;
        while (currentClass != null && foundMethod == null) {
            Method[] declaredMethods = currentClass.getDeclaredMethods();

            for (int i = 0; i < declaredMethods.length && foundMethod == null; i++) {
                Method declaredMethod = declaredMethods[i];
                Class<?>[] parameterTypes = method.getParameterTypes();
                int parameterNum = parameterTypes.length;
                if (MethodUtil.isMethodAccessibleFromClass(clazz, declaredMethod, false)
                        && parameterNum >= 2 && parameterNum <= 3) {

                    if (validateMethod(declaredMethod)) {
                        foundMethod = declaredMethod;
                    }
                }
            }

            currentClass = currentClass.getSuperclass();
        }
        return foundMethod;
    }

    private boolean validateMethod(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 3) {
            return false;
        }

        Set<Class<?>> collectedParameterTypes = new HashSet<Class<?>>();
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
