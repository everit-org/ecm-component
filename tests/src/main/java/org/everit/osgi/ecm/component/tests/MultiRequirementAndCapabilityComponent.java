/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.ecm.component.tests;

import java.util.Dictionary;
import java.util.Hashtable;

import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Deactivate;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.ServiceRefs;
import org.everit.osgi.ecm.annotation.ThreeStateBoolean;
import org.everit.osgi.ecm.component.ComponentContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Component to test multiple requirements and capabilities.
 */
@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL)
@ServiceRefs({ @ServiceRef(referenceId = "someRef", multiple = ThreeStateBoolean.TRUE,
    defaultValue = { "(service.id>=0)", "(service.id>=0)", "(service.id>=0)" }) })
@Service
public class MultiRequirementAndCapabilityComponent {

  private ServiceRegistration<String> serviceRegistration;

  @Activate
  public void activate(final ComponentContext<?> componentContext) {
    Dictionary<String, Object> properties = new Hashtable<>();
    serviceRegistration = componentContext.registerService(String.class, "hello", properties);
  }

  @Deactivate
  public void deactivate() {
    serviceRegistration.unregister();
  }
}
