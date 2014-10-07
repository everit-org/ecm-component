package org.everit.osgi.ecm.component.internal.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MethodUtil {

    private static List<String> createParameterTypeNameList(Parameter[] parameters) {
        List<String> result = new ArrayList<String>(parameters.length);

        for (Parameter parameter : parameters) {
            result.add(parameter.getType().getName());
        }
        return result;
    }

    private static MethodDefinition getMethodDefinition(String methodDefinition) {
        int indexOfOpenParams = methodDefinition.indexOf('(');

        if (indexOfOpenParams == -1) {
            return new MethodDefinition(methodDefinition, null);
        }

    }

    /**
     * See OSGi compendium spec 112.9.4. The only difference is that private methods are not allowed at all.
     */
    private static boolean isMethodAccessible(Class<?> mainClass, Method method) {
        int modifiers = method.getModifiers();

        if ((modifiers | Modifier.PRIVATE | Modifier.STATIC | Modifier.ABSTRACT) > 0) {
            return false;
        }

        if ((modifiers | Modifier.PUBLIC | Modifier.PROTECTED) > 0) {
            return true;
        }

        Class<?> declaringClass = method.getDeclaringClass();
        if (mainClass.equals(declaringClass)) {
            return true;
        }

        Package packageOfDeclaring = declaringClass.getPackage();
        Package packageOfMain = mainClass.getPackage();

        if (packageOfDeclaring == null && packageOfMain == null) {
            return true;
        }

        if (packageOfDeclaring == null || packageOfMain == null) {
            return false;
        }
        if (packageOfDeclaring.getName().equals(packageOfMain.getName())) {
            return true;
        }

        return false;
    }

    public static Method locateMethod(Class<?> clazz, String methodName, List<String[]> acceptedParameterTypesList) {
        Objects.requireNonNull(clazz, "Clazz must not be null");
        Objects.requireNonNull(methodName, "Method name must not be null");
        Objects.requireNonNull(acceptedParameterTypesList, "Accepted parameter list must not be null");

        if (acceptedParameterTypesList.size() == 0) {
            return null;
        }

        List<String> bestMatch = null;

        Map<List<String>, Method> methodsByParameters = new LinkedHashMap<List<String>, Method>();

        for (String[] preferredParameterTypes : acceptedParameterTypesList) {
            List<String> paramList = Arrays.asList(preferredParameterTypes);

            if (bestMatch == null) {
                bestMatch = paramList;
            }

            methodsByParameters.put(paramList, null);
        }

        Class<?> currentClass = clazz;

        while (currentClass != null) {
            Method[] declaredMethods = currentClass.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (method.getName().equals(methodName)) {
                    if (isMethodAccessible(clazz, method)) {
                        Parameter[] parameters = method.getParameters();
                        List<String> parameterList = createParameterTypeNameList(parameters);

                        if (bestMatch.equals(parameterList)) {
                            return method;
                        }

                        if (methodsByParameters.containsKey(parameterList)) {
                            if (methodsByParameters.get(parameterList) == null) {
                                methodsByParameters.put(parameterList, method);
                            }
                        }
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        Method selectedMethod = null;

        Collection<Method> acceptedMethods = methodsByParameters.values();
        for (Iterator<Method> iterator = acceptedMethods.iterator(); iterator.hasNext() && selectedMethod == null;) {
            Method acceptedMethod = iterator.next();
            selectedMethod = acceptedMethod;
        }
        return selectedMethod;
    }
}
