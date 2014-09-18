package org.everit.osgi.ecm.component.internal.metatype;

import java.util.ResourceBundle;

public class Localizer {

    private final ResourceBundle resourceBundle;

    public Localizer(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public String localize(String text) {
        if (text == null) {
            return null;
        }

        if (!text.startsWith("%")) {
            return text;
        }

        text = text.substring(1);

        if (text.length() == 0 || resourceBundle == null || !resourceBundle.containsKey(text)) {
            return text;
        }
        return resourceBundle.getString(text);
    }
}
