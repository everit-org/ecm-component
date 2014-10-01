package org.everit.osgi.ecm.component.internal;

import java.lang.reflect.Array;
import java.util.Dictionary;

import javax.naming.ConfigurationException;

import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.osgi.framework.Constants;

public class AttributeHelper<C, V> {

    private final AttributeMetadata<V> attributeMetadata;

    private final ComponentImpl<C> component;

    private final V[] previousValue;

    public AttributeHelper(ComponentImpl<C> component, AttributeMetadata<V> attributeMetadata) {
        this.component = component;
        this.attributeMetadata = attributeMetadata;
        previousValue = createValueArray(attributeMetadata.getValueType(), 0);
        // TODO Auto-generated constructor stub
    }

    @SuppressWarnings("unchecked")
    private V[] createValueArray(Class<V> componentType, int length) {
        return (V[]) Array.newInstance(componentType, length);
    }

    public V[] resolveNewValue(Dictionary<String, Object> properties) throws ConfigurationException {
        if (properties == null) {
            return attributeMetadata.getDefaultValue();
        }

        Object valueObject = properties.get(attributeMetadata.getAttributeId());
        if (valueObject == null) {
            if (!attributeMetadata.isOptional()) {
                throw new ConfigurationException("Mandatory attribute '" + attributeMetadata.getAttributeId()
                        + "' is not specified in component " + component.getComponentMetadata().getComponentId());
            }
            return createValueArray(attributeMetadata.getValueType(), 0);
        }

        Class<? extends Object> valueClass = valueObject.getClass();
        Class<?> componentType = valueClass;

        if (valueClass.isArray()) {
            if (!attributeMetadata.isMultiple()) {
                throw new ConfigurationException("Array value cannot be specified for the non-multiple attribute '"
                        + attributeMetadata.getAttributeId() + "' of component '"
                        + component.getComponentMetadata().getComponentId() + "' in the configuration '"
                        + properties.get(Constants.SERVICE_PID) + "'!");
            }

            componentType = valueClass.getComponentType();
        } else if (attributeMetadata.isMultiple()) {

        }

        // TODO
        return null;
    }

    public void update(Dictionary<String, Object> properties) {
        // TODO
    }

}
