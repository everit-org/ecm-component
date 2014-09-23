/**
 * This file is part of Everit - ECM Component Tests.
 *
 * Everit - ECM Component Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - ECM Component Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - ECM Component Tests.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.ecm.component.tests;

import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.attribute.IntegerAttribute;
import org.everit.osgi.ecm.annotation.attribute.IntegerAttributeOption;
import org.everit.osgi.ecm.annotation.attribute.ShortAttribute;

@Component(metatype = true, componentId = "TestAnnotedClass", configurationFactory = true,
        localizationBase = "/OSGI-INF/metatype/test")
public class AnnotatedClass {

    @ShortAttribute(multiple = true)
    private short lau;

    @IntegerAttribute(options = { @IntegerAttributeOption(label = "option 0", value = 0),
            @IntegerAttributeOption(label = "option 1", value = 1) }, defaultValue = 1)
    private int selectableInteger;

    public void setLau(short lau) {
        this.lau = lau;
    }
}
