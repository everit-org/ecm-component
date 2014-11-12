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
package org.everit.osgi.ecm.component.ri.internal.attribute;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.ri.internal.IllegalMetadataException;
import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;
import org.osgi.framework.Constants;

public class PropertyAttributeHelper<C, V> {

    private final PropertyAttributeMetadata<V> attributeMetadata;

    private final ComponentContext<C> componentContext;

    private final MethodHandle methodHandle;

    private final boolean primitive;

    public PropertyAttributeHelper(ComponentContext<C> componentContext,
            PropertyAttributeMetadata<V> attributeMetadata) {

        this.componentContext = componentContext;
        this.attributeMetadata = attributeMetadata;
        this.methodHandle = resolveMethodHandle();
        this.primitive = resolvePrimitive();

    }

    public void applyValue(Object newValue) {
        Object parameterValue = resolveValue(newValue);
        // TODO if failed than return
        if (methodHandle != null) {
            C instance = componentContext.getInstance();

            methodHandle.bindTo(instance);
            try {
                methodHandle.invoke(instance, parameterValue);
            } catch (Throwable e) {
                componentContext.fail(e, false);
            }
        }
    }

    /**
     * Converts an array to another array with the target type. This function is used to convert primitive arrays to
     * Object arrays and vice-versa.
     *
     * @param array
     *            The array object.
     * @param targetType
     *            The target type.
     * @return The converted array or null if there was a failure.
     */
    private Object convertArray(Object array, Class<?> targetType) {
        int length = Array.getLength(array);
        Object result = Array.newInstance(targetType, length);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            Array.set(result, i, element);
        }
        return result;
    }

    private void failDuringValueResolution(String message) {
        Map<String, Object> properties = componentContext.getProperties();
        String servicePid = (String) properties.get(Constants.SERVICE_PID);
        if (servicePid == null) {
            servicePid = componentContext.getComponentMetadata().getComponentId();
        }
        Throwable e = new ConfigurationException("Error during updating configuration of component '" + servicePid
                + "' that is declared in class '" + componentContext.getComponentMetadata().getType() + "': "
                + message);
        componentContext.fail(e, false);
    }

    public PropertyAttributeMetadata<V> getAttributeMetadata() {
        return attributeMetadata;
    }

    private MethodHandle resolveMethodHandle() {
        Method setter = resolveSetter();
        if (setter == null) {
            return null;
        }

        Lookup lookup = MethodHandles.lookup();
        try {
            return lookup.unreflect(setter);
        } catch (IllegalAccessException e) {
            componentContext.fail(e, true);
        }

        return null;
    }

    private boolean resolvePrimitive() {
        if (methodHandle == null) {
            return false;
        } else {
            Class<?>[] parameterArray = methodHandle.type().parameterArray();
            Class<?> parameterType = parameterArray[1];
            if (parameterType.isPrimitive()
                    || (parameterType.isArray() && parameterType.getComponentType().isPrimitive())) {
                return true;
            }
        }
        return false;
    }

    private Method resolveSetter() {
        MethodDescriptor setterMethodDescriptor = attributeMetadata.getSetter();
        if (setterMethodDescriptor == null) {
            return null;
        }
        Method method = setterMethodDescriptor.locate(componentContext.getComponentType(), false);

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throwIllegalSetter(method, "Setter method must have one parameter: " + method.toGenericString());
            return null;
        }

        if (attributeMetadata.isMultiple()) {
            if (!parameterTypes[0].isArray()) {
                throwIllegalSetter(method, "Parameter type should be an array");
                return null;
            }
            Class<?> componentType = parameterTypes[0].getComponentType();
            Class<?> expectedComponentType = attributeMetadata.getPrimitiveType();
            if (expectedComponentType == null) {
                expectedComponentType = attributeMetadata.getValueType();
            }
            if (!expectedComponentType.equals(componentType)) {
                throwIllegalSetter(method, "Parameter array should have '" + expectedComponentType
                        + "' component type.");
                return null;
            }
        } else {
            Class<?> primitiveType = attributeMetadata.getPrimitiveType();
            Class<V> valueType = attributeMetadata.getValueType();
            if (!valueType.equals(parameterTypes[0])
                    && (primitiveType == null || !primitiveType.equals(parameterTypes[0]))) {
                throwIllegalSetter(method, " Parameter type of setter must be " + valueType.getCanonicalName()
                        + ((primitiveType != null) ? (" or " + primitiveType.getSimpleName()) : ""));
                return null;
            }
        }
        return method;
    }

    private Object resolveValue(Object valueObject) {
        // Handle null value
        if (valueObject == null) {
            if (!attributeMetadata.isOptional()) {
                failDuringValueResolution("Mandatory attribute '" + attributeMetadata.getAttributeId()
                        + "' is not specified");
            }
            return null;
        }

        Class<? extends Object> valueClass = valueObject.getClass();

        Class<?> primitiveType = attributeMetadata.getPrimitiveType();
        Class<V> valueType = attributeMetadata.getValueType();

        // Handle multiple
        if (attributeMetadata.isMultiple()) {
            if (!valueClass.isArray()) {
                failDuringValueResolution("An array was expected as value for attribute '"
                        + attributeMetadata.getAttributeId() + "' but got '" + valueClass + "'");
                return null;
            }

            Class<?> componentType = valueClass.getComponentType();

            if ((primitiveType == null || !primitiveType.equals(componentType)) && !valueType.equals(componentType)) {
                StringBuilder sb = new StringBuilder();
                if (primitiveType != null) {
                    sb.append("Either '").append(primitiveType).append("[]' or ");
                }
                sb.append("'").append(valueType).append("[]' was expected for attribute '")
                        .append(attributeMetadata.getAttributeId()).append("' but got '").append(componentType)
                        .append("'");
                failDuringValueResolution(sb.toString());
                return null;
            }

            Object parameterObject = valueObject;
            if (primitive && valueType.equals(componentType)) {
                parameterObject = convertArray(parameterObject, primitiveType);
            } else if (!primitive && primitiveType != null && primitiveType.equals(componentType)) {
                parameterObject = convertArray(parameterObject, valueType);
            }
            return parameterObject;
        }

        // Handle simple value
        if (!(valueClass.equals(valueType) || (primitiveType != null && primitiveType.equals(valueClass)))) {
            Throwable e = new ConfigurationException(attributeMetadata.getValueType()
                    + " was expected as value for attribute "
                    + attributeMetadata.getAttributeId() + " of component '"
                    + componentContext.getComponentMetadata().getComponentId() + "' but got " + valueClass);
            componentContext.fail(e, false);
            return null;
        }
        return valueObject;
    }

    private void throwIllegalSetter(Method method, String additionalMessage) {
        IllegalMetadataException e = new IllegalMetadataException("Invalid setter defined for attribute '"
                + attributeMetadata.getAttributeId() + "' of component '"
                + componentContext.getComponentMetadata().getComponentId() + "'. " + additionalMessage);

        componentContext.fail(e, true);
    }

}
