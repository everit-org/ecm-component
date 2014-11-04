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
package org.everit.osgi.ecm.component.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractReferenceHolder<R> {

    private final Map<String, Object> attributes;

    private final R reference;

    private final String referenceId;

    public AbstractReferenceHolder(String referenceId, R reference, Map<String, Object> attributes) {
        this.referenceId = referenceId;
        this.reference = reference;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(attributes));
    }

    protected Map<String, Object> getAttributes() {
        return attributes;
    }

    protected R getReference() {
        return reference;
    }

    protected String getReferenceId() {
        return referenceId;
    }

}
