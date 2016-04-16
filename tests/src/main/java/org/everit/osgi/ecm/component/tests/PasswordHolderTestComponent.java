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

import java.util.Map;

import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ThreeStateBoolean;
import org.everit.osgi.ecm.annotation.attribute.PasswordAttribute;
import org.everit.osgi.ecm.annotation.attribute.PasswordAttributes;
import org.everit.osgi.ecm.component.PasswordHolder;

/**
 * Test component that has all kinds of Password attribute combinations.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
@Service
@PasswordAttributes({ @PasswordAttribute(attributeId = "noSetterPassword", defaultValue = "test"),
    @PasswordAttribute(attributeId = "noSetterMultiPassword", defaultValue = { "test1", "test2" },
        multiple = ThreeStateBoolean.TRUE) })
public class PasswordHolderTestComponent {

  private PasswordHolder[] noSetterMultiPassword;

  private PasswordHolder noSetterPassword;

  private PasswordHolder[] pshArrayPassword;

  private PasswordHolder pshPassword;

  private String simpleStringPassword;

  private String[] stringArrayPassword;

  @Activate
  public void activate(final Map<String, Object> properties) {
    noSetterPassword = (PasswordHolder) properties.get("noSetterPassword");
    noSetterMultiPassword = (PasswordHolder[]) properties.get("noSetterMultiPassword");
  }

  public PasswordHolder[] getNoSetterMultiPassword() {
    return noSetterMultiPassword.clone();
  }

  public PasswordHolder getNoSetterPassword() {
    return noSetterPassword;
  }

  public PasswordHolder[] getPshArrayPassword() {
    return pshArrayPassword.clone();
  }

  public PasswordHolder getPshPassword() {
    return pshPassword;
  }

  public String getSimpleStringPassword() {
    return simpleStringPassword;
  }

  public String[] getStringArrayPassword() {
    return stringArrayPassword.clone();
  }

  @PasswordAttribute(defaultValue = { "test1", "test2" })
  public void setPshArrayPassword(final PasswordHolder[] pshArrayPassword) {
    this.pshArrayPassword = pshArrayPassword;
  }

  @PasswordAttribute(defaultValue = "test")
  public void setPshPassword(final PasswordHolder pshPassword) {
    this.pshPassword = pshPassword;
  }

  @PasswordAttribute(defaultValue = "test")
  public void setSimpleStringPassword(final String simpleStringPassword) {
    this.simpleStringPassword = simpleStringPassword;
  }

  @PasswordAttribute(defaultValue = { "test1", "test2" })
  public void setStringArrayPassword(final String[] stringArrayPassword) {
    this.stringArrayPassword = stringArrayPassword;
  }
}
