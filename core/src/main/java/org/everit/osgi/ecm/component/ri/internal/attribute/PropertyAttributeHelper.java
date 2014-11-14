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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.ri.internal.IllegalMetadataException;
import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;
import org.osgi.framework.Constants;

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

    private final ComponentContext<C> componentContext;

    private final MethodHandle methodHandle;

    private final boolean primitive;

    public PropertyAttributeHelper(ComponentContext<C> componentContext,
            PropertyAttributeMetadata<V_ARRAY> attributeMetadata) {

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

    public PropertyAttributeMetadata<V_ARRAY> getAttributeMetadata() {
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
            Class<?> expectedComponentType = attributeMetadata.getValueType();

            if (!expectedComponentType.equals(componentType)) {
                throwIllegalSetter(method, "Parameter array should have '" + expectedComponentType
                        + "' component type.");
                return null;
            }
        } else {
            Class<?> valueType = attributeMetadata.getValueType();
            if (!valueType.equals(parameterTypes[0])) {
                if (valueType.isPrimitive()) {
                    Class<?> boxingType = PRIMITIVE_BOXING_TYPE_MAPPING.get(valueType);
                    if (!boxingType.equals(parameterTypes[0])) {
                        throwIllegalSetter(
                                method, " Parameter should be " + valueType.getCanonicalName() + " or "
                                        + boxingType.getCanonicalName() + ": " + method.toGenericString());
                    }
                } else {
                    throwIllegalSetter(
                            method, " Parameter type should be " + valueType.getCanonicalName() + ": "
                                    + method.toGenericString());
                }
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

        Class<?> attributeType = attributeMetadata.getValueType();

        // Handle multiple
        if (attributeMetadata.isMultiple()) {
            if (!valueClass.isArray()) {
                failDuringValueResolution("An array was expected as value for attribute '"
                        + attributeMetadata.getAttributeId() + "' but got '" + valueClass + "'");
                return null;
            }

            Class<?> componentType = valueClass.getComponentType();

            if (!attributeType.equals(componentType)) {
                StringBuilder sb = new StringBuilder();
                sb.append("'").append(attributeType).append("[]' was expected for attribute '")
                        .append(attributeMetadata.getAttributeId()).append("' but got '").append(componentType)
                        .append("'");
                failDuringValueResolution(sb.toString());
                return null;
            }

            return valueObject;
        }

        // Handle simple value
        if (!valueClass.equals(attributeType)
                && (!attributeType.isPrimitive() || !PRIMITIVE_BOXING_TYPE_MAPPING.get(attributeType)
                        .equals(valueClass))) {

            StringBuilder sb = new StringBuilder();
            if (attributeType.isPrimitive()) {
                sb.append("Either ").append(PRIMITIVE_BOXING_TYPE_MAPPING.get(attributeType).getCanonicalName())
                        .append(" or ");
            }
            sb.append(attributeType.getCanonicalName()).append(" was expected but got ")
                    .append(valueClass.getCanonicalName());
            failDuringValueResolution(sb.toString());
            return null;
        }
        return valueObject;
    }

    private void throwIllegalSetter(Method method, String additionalMessage) {
        IllegalMetadataException e = new IllegalMetadataException("Invalid setter '" + method.toGenericString()
                + "' defined for attribute '" + attributeMetadata.getAttributeId() + "' of component '"
                + componentContext.getComponentMetadata().getComponentId() + "'. " + additionalMessage);

        componentContext.fail(e, true);
    }

}
