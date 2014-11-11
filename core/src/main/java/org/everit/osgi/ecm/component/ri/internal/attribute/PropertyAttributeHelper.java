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

import javax.naming.ConfigurationException;

import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.ri.internal.IllegalMetadataException;
import org.everit.osgi.ecm.metadata.PropertyAttributeMetadata;
import org.everit.osgi.ecm.util.method.MethodDescriptor;

public class PropertyAttributeHelper<C, V> {

    private final PropertyAttributeMetadata<V> attributeMetadata;

    private final ComponentContext<C> componentContext;

    private final MethodHandle methodHandle;

    public PropertyAttributeHelper(ComponentContext<C> componentContext,
            PropertyAttributeMetadata<V> attributeMetadata) {

        this.componentContext = componentContext;
        this.attributeMetadata = attributeMetadata;
        this.methodHandle = resolveMethodHandle();
    }

    public void applyValue(Object newValue) {
        if (methodHandle != null) {
            C instance = componentContext.getInstance();

            methodHandle.bindTo(instance);
            try {
                methodHandle.invoke(instance, newValue);
            } catch (Throwable e) {
                componentContext.fail(e, false);
            }
        }
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

    private void throwIllegalSetter(Method method, String additionalMessage) {
        IllegalMetadataException e = new IllegalMetadataException("Invalid setter defined for attribute '"
                + attributeMetadata.getAttributeId() + "' of component '"
                + componentContext.getComponentMetadata().getComponentId() + "'. " + additionalMessage);

        componentContext.fail(e, true);
    }

    public void validate(Object valueObject) throws ConfigurationException {
        // Handle null value
        if (valueObject == null) {
            if (!attributeMetadata.isOptional()) {
                throw new ConfigurationException("Mandatory attribute '" + attributeMetadata.getAttributeId()
                        + "' is not specified in component "
                        + componentContext.getComponentMetadata().getComponentId());
            }
            return;
        }

        Class<? extends Object> valueClass = valueObject.getClass();

        // Handle multiple
        if (attributeMetadata.isMultiple()) {
            if (!valueClass.isArray()) {
                throw new ConfigurationException("Array was expected as value for attribute "
                        + attributeMetadata.getAttributeId() + " of component '"
                        + componentContext.getComponentMetadata().getComponentId() + "' but got '" + valueClass + "'");
            }

            Class<?> componentType = valueClass.getComponentType();
            Class<?> primitiveType = attributeMetadata.getPrimitiveType();
            if (primitiveType == null) {
                primitiveType = attributeMetadata.getValueType();
            }

            if (!primitiveType.equals(componentType)) {
                throw new ConfigurationException(primitiveType + " array was expected as value for attribute "
                        + attributeMetadata.getAttributeId() + " of component '"
                        + componentContext.getComponentMetadata().getComponentId() + "' but got " + componentType
                        + " array");
            }
            return;
        }

        // Handle simple value
        if (!valueClass.equals(attributeMetadata.getValueType())) {
            throw new ConfigurationException(attributeMetadata.getValueType() + " was expected as value for attribute "
                    + attributeMetadata.getAttributeId() + " of component '"
                    + componentContext.getComponentMetadata().getComponentId() + "' but got " + valueClass);
        }
        return;
    }
}
