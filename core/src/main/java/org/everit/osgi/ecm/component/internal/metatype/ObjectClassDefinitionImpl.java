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

import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.Icon;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

public class ObjectClassDefinitionImpl<C> implements ObjectClassDefinition {

    private final ClassLoader classLoader;

    private final ComponentMetadata<C> componentMetadata;

    private final Localizer localizer;

    public ObjectClassDefinitionImpl(ComponentMetadata<C> componentMetadata, Localizer localizer,
            ClassLoader classLoader) {
        this.componentMetadata = componentMetadata;
        this.localizer = localizer;
        this.classLoader = classLoader;
    }

    @Override
    public AttributeDefinition[] getAttributeDefinitions(int filter) {
        // TODO Auto-generated method stub
        return new AttributeDefinition[] { new AttributeDefinitionImpl() };
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
        return localizer.localize(componentMetadata.getName());
    }

}
