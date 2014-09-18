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

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class ManagedServiceFactoryImpl implements ManagedServiceFactory {

    @Override
    public String getName() {
        System.out.println("ManagedServiceFactory getName called");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        System.out
                .println("ManagedServiceFactory updated called: pid=" + pid + ", properties=" + properties.toString());
        // TODO Auto-generated method stub

    }

    @Override
    public void deleted(String pid) {
        System.out.println("ManagedServiceFactory deleted called with pid: " + pid);
        // TODO Auto-generated method stub

    }

}
