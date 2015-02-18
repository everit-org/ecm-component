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

import java.util.Dictionary;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

class ComponentServiceRegistration<S, C> implements ServiceRegistration<S> {

    /**
     *
     */
    private final ComponentContextImpl<C> component;
    private final ServiceRegistration<S> wrapped;

    public ComponentServiceRegistration(final ComponentContextImpl<C> component, final ServiceRegistration<S> wrapped) {
        this.component = component;
        this.wrapped = wrapped;
    }

    @Override
    public ServiceReference<S> getReference() {
        return wrapped.getReference();
    }

    @Override
    public void setProperties(final Dictionary<String, ?> properties) {
        wrapped.setProperties(properties);
    }

    @Override
    public void unregister() {
        this.component.removeServiceRegistration(this);
        wrapped.unregister();
    }

}
