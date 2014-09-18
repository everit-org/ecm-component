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
        return null;
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
