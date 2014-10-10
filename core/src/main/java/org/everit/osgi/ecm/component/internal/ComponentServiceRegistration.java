package org.everit.osgi.ecm.component.internal;

import java.util.Dictionary;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

class ComponentServiceRegistration<S, C> implements ServiceRegistration<S> {

    /**
     *
     */
    private final Component<C> component;
    private final ServiceRegistration<S> wrapped;

    public ComponentServiceRegistration(Component<C> component, ServiceRegistration<S> wrapped) {
        this.component = component;
        this.wrapped = wrapped;
    }

    @Override
    public ServiceReference<S> getReference() {
        return wrapped.getReference();
    }

    @Override
    public void setProperties(Dictionary<String, ?> properties) {
        wrapped.setProperties(properties);
    }

    @Override
    public void unregister() {
        this.component.registeredServices.remove(this);
        wrapped.unregister();
    }

}
