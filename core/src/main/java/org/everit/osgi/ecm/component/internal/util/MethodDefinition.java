package org.everit.osgi.ecm.component.internal.util;


public class MethodDefinition {

    private final String methodName;

    private final Class<?>[] parameterTypes;

    public MethodDefinition(String methodName, Class<?>[] parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }
}
