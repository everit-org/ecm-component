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
