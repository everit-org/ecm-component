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
package org.everit.osgi.ecm.component.internal.metatype;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.everit.osgi.ecm.component.internal.Localizer;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.Icon;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;
import org.everit.osgi.ecm.metadata.StringAttributeMetadata;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

public class ObjectClassDefinitionImpl<C> implements ObjectClassDefinition {

    private final AttributeDefinition[] attributeDefinitions;

    private final ClassLoader classLoader;

    private final ComponentMetadata<C> componentMetadata;

    private final Localizer localizer;

    public ObjectClassDefinitionImpl(ComponentMetadata<C> componentMetadata, Localizer localizer,
            ClassLoader classLoader) {
        this.componentMetadata = componentMetadata;
        this.localizer = localizer;
        this.classLoader = classLoader;

        attributeDefinitions = createAttributeDefinitions(componentMetadata);

    }

    private AttributeDefinition[] createAttributeDefinitions(ComponentMetadata<C> componentMetadata) {
        AttributeMetadata<?>[] attributes = componentMetadata.getAttributes();
        if (attributes == null || attributes.length == 0) {
            return null;
        }

        List<AttributeDefinition> result = new LinkedList<AttributeDefinition>();

        for (AttributeMetadata<?> attribute : attributes) {
            if (attribute instanceof ReferenceMetadata) {
                // TODO
            } else if (attribute instanceof StringAttributeMetadata) {
                // TODO
            } else {
                result.add(new AttributeDefinitionImpl(attribute, localizer));
            }
        }

        if (result.size() == 0) {
            return null;
        }

        return result.toArray(new AttributeDefinition[result.size()]);
    }

    @Override
    public AttributeDefinition[] getAttributeDefinitions(int filter) {
        if (filter == ObjectClassDefinition.ALL || filter == ObjectClassDefinition.REQUIRED) {
            return attributeDefinitions;
        } else {
            // TODO handle optional attributes
            return null;
        }
    }

    @Override
    public String getDescription() {
        return localizer.localize(componentMetadata.getDescription());
    }

    @Override
    public InputStream getIcon(int size) throws IOException {
        Icon[] icons = componentMetadata.getIcons();
        if (icons == null || icons.length == 0) {
            return null;
        }

        int difference = Integer.MAX_VALUE;
        Icon selectedIcon = null;
        for (Icon icon : icons) {
            int currentDifference = Math.abs(size - icon.getSize());
            if (currentDifference < difference) {
                selectedIcon = icon;
            }
        }

        String iconPath = localizer.localize(selectedIcon.getPath());
        if (iconPath != null) {
            classLoader.getResourceAsStream(iconPath);
            return null;
        } else {
            return null;
        }
    }

    @Override
    public String getID() {
        return componentMetadata.getConfigurationPid();
    }

    @Override
    public String getName() {
        return localizer.localize(componentMetadata.getComponentId());
    }

}
