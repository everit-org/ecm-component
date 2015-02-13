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
package org.everit.osgi.ecm.component.ri.internal;

import org.everit.osgi.ecm.component.ri.internal.attribute.ReferenceHelper;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;

public interface ReferenceEventHandler {

    void updateWithoutSatisfactionChange(ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper);

    void updateNonDynamic(ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper);

    void satisfied(ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper);

    void unsatisfied(ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper);
}
