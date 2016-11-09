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
import java.util.Map;

import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.IntegerAttribute;
import org.everit.osgi.ecm.annotation.attribute.IntegerAttributeOption;
import org.everit.osgi.ecm.annotation.attribute.PasswordAttribute;
import org.everit.osgi.ecm.annotation.attribute.ShortAttribute;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.osgi.framework.Constants;
import org.osgi.service.metatype.MetaTypeService;

/**
 * Component for testing Factory functionality.
 */
@Component(metatype = true, componentId = "TestFactoryClass",
    configurationPolicy = ConfigurationPolicy.FACTORY,
    localizationBase = "/OSGI-INF/metatype/test")
@Service
public class FactoryComponent {

  @ShortAttribute
  private short lau;

  @PasswordAttribute
  private String password;

  @IntegerAttribute(options = { @IntegerAttributeOption(label = "option 0", value = 0),
      @IntegerAttributeOption(label = "option 1", value = 1) }, defaultValue = 1)
  private int selectableInteger;

  @ServiceRef
  private MetaTypeService someReference;

  /**
   * Activator of the component that registers a new service.
   */
  @Activate
  public void activate(final ComponentContext<FactoryComponent> context) {
    Dictionary<String, Object> properties = new Hashtable<>();
    ComponentRevision<FactoryComponent> componentRevision = context.getComponentRevision();
    Map<String, Object> componentProperties = componentRevision.getProperties();
    Object servicePid = componentProperties.get(Constants.SERVICE_PID);
    properties.put("service.pid", servicePid);
    context.registerService(String.class, "testService", properties);
  }

  public short getLau() {
    return lau;
  }

  public String getPassword() {
    return password;
  }

  public int getSelectableInteger() {
    return selectableInteger;
  }

  public MetaTypeService getSomeReference() {
    return someReference;
  }
}
