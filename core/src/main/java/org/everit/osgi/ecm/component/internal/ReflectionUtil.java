package org.everit.osgi.ecm.component.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReflectionUtil {

    public static Collection<Method> getAccessibleMethodsByName(Class<?> clazz, String methodName) {
        Set<Class<?>> processedClasses = new HashSet<Class<?>>();

        Map<List<Class<?>>, Method> methodsByParameters = new HashMap<List<Class<?>>, Method>();

        Class<?> currentClass = clazz;

        while (currentClass != null) {
            Method[] declaredMethods = currentClass.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (method.getName().equals(methodName)) {
                    if (isMethodAccessible(clazz, method)) {
                        Parameter[] parameters = method.getParameters();
                        List<Parameter> parameterList = Arrays.asList(parameters);

                        methodsByParameters.get(key)
                    }
                }
            }
        }

        return methodsByParameters.values();

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
}
