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

import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;

/**
 * Simple empty component.
 */
@Component(
    componentId = SimpleComponent.COMPONENT_ID,
    configurationPolicy = ConfigurationPolicy.REQUIRE)
@Service
public class SimpleComponent {

  protected static final String COMPONENT_ID = "SimpleComponent";

  @StringAttribute
  private String simpleStringAttr;

  public String getComponentId() {
    return COMPONENT_ID;
  }

  public String getSimpleStringAttr() {
    return simpleStringAttr;
  }

  public void setSimpleStringAttr(final String simpleStringAttr) {
    this.simpleStringAttr = simpleStringAttr;
  }

}
