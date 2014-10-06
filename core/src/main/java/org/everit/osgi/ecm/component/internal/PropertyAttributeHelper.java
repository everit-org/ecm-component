package org.everit.osgi.ecm.component.internal;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Dictionary;

import javax.naming.ConfigurationException;

import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;

public class PropertyAttributeHelper<C, V> {

    private final PropertyAttributeMetadata<V> attributeMetadata;

    private final ComponentImpl<C> component;

    private Object storedValue = null;

    private final Object defaultValue;

    private final Method setter;

    public PropertyAttributeHelper(ComponentImpl<C> component, PropertyAttributeMetadata<V> attributeMetadata) {
        this.component = component;
        this.attributeMetadata = attributeMetadata;
        this.defaultValue = createDefaultValue();
        this.setter = resolveSetter();
    }

    private Method resolveSetter() {
        String setterName = attributeMetadata.getSetter();
        if (setterName == null) {
            return null;
        }
        Class<C> componentType = component.getComponentType();

        try {
            return componentType.getMethod(setterName, attributeMetadata.getPrimitiveType());
        } catch (NoSuchMethodException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            return componentType.getMethod(setterName, attributeMetadata.getValueType());
        } catch (NoSuchMethodException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private Object createDefaultValue() {
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

    private Object convertToPrimitiveArray(V[] array, Class<?> primitiveType) {
        Object primitiveArray = Array.newInstance(primitiveType, array.length);
        for (int i = 0; i < array.length; i++) {
            V element = array[i];
            Array.set(primitiveArray, i, element);
        }
        return primitiveArray;
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

    public Object getStoredValue() {
        return storedValue;
    }

}
