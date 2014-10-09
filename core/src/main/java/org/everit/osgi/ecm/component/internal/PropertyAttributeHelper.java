package org.everit.osgi.ecm.component.internal;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Dictionary;

import javax.naming.ConfigurationException;

import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;

public class PropertyAttributeHelper<C, V> {

    private final PropertyAttributeMetadata<V> attributeMetadata;

    private final ComponentImpl<C> component;

    private final Object defaultValue;

    private final Method setter;

    private Object storedValue = null;

    public PropertyAttributeHelper(ComponentImpl<C> component, PropertyAttributeMetadata<V> attributeMetadata) {
        this.component = component;
        this.attributeMetadata = attributeMetadata;
        this.defaultValue = resolveDefaultValue();
        this.setter = resolveSetter();
    }

    public void applyValue(Object newValue) {
        storedValue = newValue;
        if (setter != null) {
            try {
                setter.invoke(component.getInstance(), newValue);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException("Error setting attribute " + attributeMetadata.getAttributeId()
                        + " of component " + component.getComponentMetadata().getComponentId() + " by calling setter "
                        + setter.toGenericString() + " with value: " + newValue, e);
            }
        }
    }

    private Object convertToPrimitiveArray(V[] array, Class<?> primitiveType) {
        Object primitiveArray = Array.newInstance(primitiveType, array.length);
        for (int i = 0; i < array.length; i++) {
            V element = array[i];
            Array.set(primitiveArray, i, element);
        }
        return primitiveArray;
    }

    private Object resolveDefaultValue() {
        V[] defaultValueArray = attributeMetadata.getDefaultValue();
        if (defaultValueArray == null) {
            return null;
        }

        if (attributeMetadata.isMultiple()) {
            Class<?> primitiveType = attributeMetadata.getPrimitiveType();
            if (primitiveType != null) {
                return convertToPrimitiveArray(defaultValueArray, primitiveType);
            }
            return defaultValueArray;
        }

        return defaultValueArray[0];
    }

    public Object getStoredValue() {
        return storedValue;
    }

    public boolean newValueEqualsPrevious(Object newValue) {
        if (newValue == null && storedValue == null) {
            return true;
        }

        if (newValue == null || storedValue == null) {
            return false;
        }

        Class<? extends Object> valueType = newValue.getClass();
        if (!valueType.isArray()) {
            return newValue.equals(storedValue);
        }

        Class<?> componentType = valueType.getComponentType();

        boolean primitiveArray = componentType.isPrimitive();
        if (!primitiveArray) {
            return Arrays.equals((Object[]) newValue, (Object[]) storedValue);
        }

        Class<Arrays> arraysClass = Arrays.class;

        try {
            Method equalsMethod = arraysClass.getMethod("equals", valueType, valueType);
            return (Boolean) equalsMethod.invoke(Arrays.class, newValue, storedValue);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

    public Object resolveNewValue(Dictionary<String, Object> properties) throws ConfigurationException {
        // Handle default value
        if (properties == null) {
            if (defaultValue == null && !attributeMetadata.isOptional()) {
                throw new ConfigurationException("No configuration and no default value for attribute '"
                        + attributeMetadata.getAttributeId() + "' of component "
                        + component.getComponentMetadata().getComponentId());
            }
            return defaultValue;
        }

        // Handle null value
        Object valueObject = properties.get(attributeMetadata.getAttributeId());
        if (valueObject == null) {
            if (!attributeMetadata.isOptional()) {
                throw new ConfigurationException("Mandatory attribute '" + attributeMetadata.getAttributeId()
                        + "' is not specified in component " + component.getComponentMetadata().getComponentId());
            }
            return null;
        }

        Class<? extends Object> valueClass = valueObject.getClass();

        // Handle multiple
        if (attributeMetadata.isMultiple()) {
            if (!valueClass.isArray()) {
                throw new ConfigurationException("Array was expected as value for attribute "
                        + attributeMetadata.getAttributeId() + " of component '"
                        + component.getComponentMetadata().getComponentId() + "' but got '" + valueClass + "'");
            }

            Class<?> componentType = valueClass.getComponentType();
            Class<?> primitiveType = attributeMetadata.getPrimitiveType();
            if (primitiveType == null) {
                primitiveType = attributeMetadata.getValueType();
            }

            if (!primitiveType.equals(componentType)) {
                throw new ConfigurationException(primitiveType + " array was expected as value for attribute "
                        + attributeMetadata.getAttributeId() + " of component '"
                        + component.getComponentMetadata().getComponentId() + "' but got " + componentType + " array");
            }
            return valueObject;
        }

        // Handle simple value
        if (!valueClass.equals(attributeMetadata.getValueType())) {
            throw new ConfigurationException(attributeMetadata.getValueType() + " was expected as value for attribute "
                    + attributeMetadata.getAttributeId() + " of component '"
                    + component.getComponentMetadata().getComponentId() + "' but got " + valueClass);
        }
        return valueObject;
    }

    private Method resolveSetter() {
        MethodDescriptor setterMethodDescriptor = attributeMetadata.getSetter();
        if (setterMethodDescriptor == null) {
            return null;
        }
        Method method = setterMethodDescriptor.locate(component.getComponentType(), false);

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throwIllegalSetter(method, "Setter method must have one parameter: " + method.toGenericString());
        }

        if (attributeMetadata.isMultiple()) {
            if (!parameterTypes[0].isArray()) {
                throwIllegalSetter(method, "Parameter type should be an array");
            }
            Class<?> componentType = parameterTypes[0].getComponentType();
            Class<?> expectedComponentType = attributeMetadata.getPrimitiveType();
            if (expectedComponentType == null) {
                expectedComponentType = attributeMetadata.getValueType();
            }
            if (!expectedComponentType.equals(componentType)) {
                throwIllegalSetter(method, "Parameter array should have '" + expectedComponentType
                        + "' component type.");
            }
        } else {
            Class<?> primitiveType = attributeMetadata.getPrimitiveType();
            Class<V> valueType = attributeMetadata.getValueType();
            if (!valueType.equals(parameterTypes[0])
                    && (primitiveType == null || !primitiveType.equals(parameterTypes[0]))) {
                throwIllegalSetter(method, " Parameter type of setter must be " + valueType.getCanonicalName()
                        + ((primitiveType != null) ? (" or " + primitiveType.getSimpleName()) : ""));
            }
        }
        return method;
    }

    private void throwIllegalSetter(Method method, String additionalMessage) {
        throw new IllegalMetadataException("Invalid setter defined for attribute '"
                + attributeMetadata.getAttributeId() + "' of component '"
                + component.getComponentMetadata().getComponentId() + "'. " + additionalMessage);
    }
}
