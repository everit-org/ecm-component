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

import org.osgi.service.metatype.AttributeDefinition;

public class AttributeDefinitionImpl implements AttributeDefinition {

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "x";
    }

    @Override
    public String getID() {
        // TODO Auto-generated method stub
        return "x";
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return "x";
    }

    @Override
    public int getCardinality() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getType() {
        // TODO Auto-generated method stub
        return AttributeDefinition.STRING;
    }

    @Override
    public String[] getOptionValues() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getOptionLabels() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String validate(String value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getDefaultValue() {
        // TODO Auto-generated method stub
        return null;
    }

}
