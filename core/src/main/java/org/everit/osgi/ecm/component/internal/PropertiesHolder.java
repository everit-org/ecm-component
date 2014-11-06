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
package org.everit.osgi.ecm.component.internal;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class PropertiesHolder {

    private final Dictionary<String, Object> wrappedDictionary;;

    private volatile Map<String, Object> wrappedMap;

    public PropertiesHolder(Dictionary<String, Object> wrapped) {
        this.wrappedDictionary = wrapped;
    }

    public Dictionary<String, Object> getDictionary() {
        return wrappedDictionary;
    }

    public Map<String, Object> getMap() {
        if (wrappedDictionary == null) {
            return null;
        }

        if (wrappedMap == null) {
            initMap();
        }
        return wrappedMap;
    }

    private synchronized void initMap() {
        if (wrappedMap != null) {
            return;
        }

        if (wrappedDictionary instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> tmpMap = (Map<String, Object>) wrappedDictionary;
            wrappedMap = Collections.unmodifiableMap(new HashMap<String, Object>(tmpMap));
        } else {
            HashMap<String, Object> tmpMap = new HashMap<String, Object>();
            Enumeration<String> keys = wrappedDictionary.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                tmpMap.put(key, wrappedDictionary.get(key));
            }
            wrappedMap = Collections.unmodifiableMap(tmpMap);
        }

    }
}
