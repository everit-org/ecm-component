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

import java.util.Dictionary;
import java.util.Map;

import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

public interface ComponentContext<C> {

    /**
     * Causes the component to step into the {@link ComponentState#FAILED} or {@link ComponentState#FAILED_PERMANENT}
     * state.
     *
     * @param e
     *            The cause of the failure.
     * @param permanent
     *            Whether the failure should be permanent or not. In case of permanent failure, the component will not
     *            be restarted until the bundle that registered the component is updated.
     */
    void fail(Throwable e, boolean permanent);

    BundleContext getBundleContext();

    ComponentMetadata getComponentMetadata();

    ComponentRevision getComponentRevision();

    /**
     * Get the service that was registered when the component was opened. The service reference can be used to log
     * messages via {@link LogService}.
     *
     * @return The {@link ServiceReference} that offers the {@link ComponentContainer} and optionally interfaces.
     */
    ServiceReference<?> getComponentServiceReference();

    Class<C> getComponentType();

    /**
     * Returns the service instance that was instantiated by this component.
     *
     * @return The instance of the component or <code>null</code> if the component is not in
     *         {@link ComponentState#ACTIVE}.
     */
    C getInstance();

    Map<String, Object> getProperties();

    /**
     * Registers the specified service object with the specified properties under the name of the specified class with
     * the Framework.
     *
     * <p>
     * This method is otherwise identical to {@link #registerService(String, Object, Dictionary)} and is provided to
     * return a type safe {@code ServiceRegistration}.
     *
     * @param <S>
     *            Type of Service.
     * @param clazz
     *            The class under whose name the service can be located.
     * @param service
     *            The service object or a {@code ServiceFactory} object.
     * @param properties
     *            The properties for this service.
     * @return A {@code ServiceRegistration} object for use by the bundle registering the service to update the
     *         service's properties or to unregister the service.
     * @throws IllegalStateException
     *             If this BundleContext is no longer valid.
     * @see #registerService(String, Object, Dictionary)
     * @since 1.6
     */
    <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties);

    /**
     * Registers the specified service object with the specified properties under the specified class name with the
     * Framework.
     *
     * <p>
     * This method is otherwise identical to {@link #registerService(String[], Object, Dictionary)} and is provided as a
     * convenience when {@code service} will only be registered under a single class name. Note that even in this case
     * the value of the service's {@link Constants#OBJECTCLASS} property will be an array of string, rather than just a
     * single string.
     *
     * @param clazz
     *            The class name under which the service can be located.
     * @param service
     *            The service object or a {@code ServiceFactory} object.
     * @param properties
     *            The properties for this service.
     * @return A {@code ServiceRegistration} object for use by the bundle registering the service to update the
     *         service's properties or to unregister the service.
     * @throws IllegalStateException
     *             If this BundleContext is no longer valid.
     * @see #registerService(String[], Object, Dictionary)
     */
    ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties);

    /**
     * Registers the specified service object with the specified properties under the specified class names into the
     * Framework. A {@code ServiceRegistration} object is returned. The {@code ServiceRegistration} object is for the
     * private use of the bundle registering the service and should not be shared with other bundles. The registering
     * bundle is defined to be the context bundle. Other bundles can locate the service by using one of the
     * {@link #getServiceReferences(Class, String)}, {@link #getServiceReferences(String, String)},
     * {@link #getServiceReference(Class)} or {@link #getServiceReference(String)} methods.
     *
     * <p>
     * A bundle can register a service object that implements the {@link ServiceFactory} interface to have more
     * flexibility in providing service objects to other bundles.
     *
     * <p>
     * The following steps are required to register a service:
     * <ol>
     * <li>If {@code service} is not a {@code ServiceFactory}, an {@code IllegalArgumentException} is thrown if
     * {@code service} is not an {@code instanceof} all the specified class names.
     * <li>The Framework adds the following service properties to the service properties from the specified
     * {@code Dictionary} (which may be {@code null}): <br/>
     * A property named {@link Constants#SERVICE_ID} identifying the registration number of the service <br/>
     * A property named {@link Constants#OBJECTCLASS} containing all the specified classes. <br/>
     * Properties with these names in the specified {@code Dictionary} will be ignored.
     * <li>The service is added to the Framework service registry and may now be used by other bundles.
     * <li>A service event of type {@link ServiceEvent#REGISTERED} is fired.
     * <li>A {@code ServiceRegistration} object for this registration is returned.
     * </ol>
     *
     * @param clazzes
     *            The class names under which the service can be located. The class names in this array will be stored
     *            in the service's properties under the key {@link Constants#OBJECTCLASS}.
     * @param service
     *            The service object or a {@code ServiceFactory} object.
     * @param properties
     *            The properties for this service. The keys in the properties object must all be {@code String} objects.
     *            See {@link Constants} for a list of standard service property keys. Changes should not be made to this
     *            object after calling this method. To update the service's properties the
     *            {@link ServiceRegistration#setProperties(Dictionary)} method must be called. The set of properties may
     *            be {@code null} if the service has no properties.
     * @return A {@code ServiceRegistration} object for use by the bundle registering the service to update the
     *         service's properties or to unregister the service.
     * @throws IllegalArgumentException
     *             If one of the following is true:
     *             <ul>
     *             <li>{@code service} is {@code null}. <li>{@code service} is not a {@code ServiceFactory} object and
     *             is not an instance of all the named classes in {@code clazzes}. <li> {@code properties} contains case
     *             variants of the same key name.
     *             </ul>
     * @throws SecurityException
     *             If the caller does not have the {@code ServicePermission} to register the service for all the named
     *             classes and the Java Runtime Environment supports permissions.
     * @throws IllegalStateException
     *             If this BundleContext is no longer valid.
     * @see ServiceRegistration
     * @see ServiceFactory
     */
    ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties);

    /**
     * Causes the restarting of the component. The old instance will be dropped and a new one will be instantiated.
     */
    void restart();
}
