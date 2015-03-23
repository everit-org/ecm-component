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
import org.everit.osgi.ecm.annotation.Deactivate;
import org.everit.osgi.ecm.annotation.ReferenceConfigurationType;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.ThreeStateBoolean;
import org.everit.osgi.ecm.annotation.attribute.BooleanAttribute;
import org.everit.osgi.ecm.annotation.attribute.BooleanAttributes;
import org.everit.osgi.ecm.annotation.attribute.ByteAttribute;
import org.everit.osgi.ecm.annotation.attribute.CharacterAttribute;
import org.everit.osgi.ecm.annotation.attribute.DoubleAttribute;
import org.everit.osgi.ecm.annotation.attribute.FloatAttribute;
import org.everit.osgi.ecm.annotation.attribute.IntegerAttribute;
import org.everit.osgi.ecm.annotation.attribute.LongAttribute;
import org.everit.osgi.ecm.annotation.attribute.PasswordAttribute;
import org.everit.osgi.ecm.annotation.attribute.ShortAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.ServiceHolder;
import org.osgi.service.cm.ManagedService;

/**
 * Component to test initialization via setters and activate method.
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@BooleanAttributes({ @BooleanAttribute(attributeId = "booleanArrayAttribute",
    multiple = ThreeStateBoolean.TRUE,
    defaultValue = { false, false, true }) })
@Service
public class TestComponent {

  /**
   * Variable is initialized from Activate method.
   */
  private boolean[] booleanArrayAttribute;

  @BooleanAttribute(setter = "bindBooleanAttribute")
  private Boolean booleanAttribute;

  @ByteAttribute
  private byte[] byteArrayAttribute;

  @ByteAttribute
  private byte byteAttribute;

  @CharacterAttribute
  private char[] charArrayAttribute;

  @CharacterAttribute
  private char charAttribute;

  @DoubleAttribute
  private double[] doubleArrayAttribute;

  @DoubleAttribute
  private double doubleAttribute;

  @FloatAttribute
  private float[] floatArrayAttribute;

  @FloatAttribute
  private float floatAttribute;
  /**
   * Annotation is defined on setter.
   */
  private int[] intArrayAttribute;

  @IntegerAttribute(defaultValue = 1)
  private int intAttribute;

  @LongAttribute
  private long[] longArrayAttribute;

  @LongAttribute
  private long longAttribute;

  @PasswordAttribute
  private String[] passwordArrayAttribute;

  @PasswordAttribute
  private String passwordAttribute;

  @ShortAttribute
  private short[] shortArrayAttribute;

  @ShortAttribute
  private short shortAttribute;

  @StringAttribute
  private String[] stringArrayAttribute;

  @StringAttribute(dynamic = true)
  private String stringAttribute;

  /**
   * Activate method that sets {@link #booleanArrayAttribute} based on componentContext.
   */
  @Activate
  public void activate(final ComponentContext<TestComponent> componentContext) {
    Map<String, Object> properties = componentContext.getComponentRevision().getProperties();
    Object booleanArrayAttribute = properties.get("booleanArrayAttribute");
    this.booleanArrayAttribute = (boolean[]) booleanArrayAttribute;
  }

  public void bindBooleanAttribute(final Boolean booleanAttribute) {
    this.booleanAttribute = booleanAttribute;
  }

  @Deactivate
  public void deactivate() {

  }

  public boolean[] getBooleanArrayAttribute() {
    return booleanArrayAttribute;
  }

  public Boolean getBooleanAttribute() {
    return booleanAttribute;
  }

  public byte[] getByteArrayAttribute() {
    return byteArrayAttribute;
  }

  public byte getByteAttribute() {
    return byteAttribute;
  }

  public char[] getCharArrayAttribute() {
    return charArrayAttribute;
  }

  public char getCharAttribute() {
    return charAttribute;
  }

  public double[] getDoubleArrayAttribute() {
    return doubleArrayAttribute;
  }

  public double getDoubleAttribute() {
    return doubleAttribute;
  }

  public float[] getFloatArrayAttribute() {
    return floatArrayAttribute;
  }

  public float getFloatAttribute() {
    return floatAttribute;
  }

  public int[] getIntArrayAttribute() {
    return intArrayAttribute;
  }

  public int getIntAttribute() {
    return intAttribute;
  }

  public long[] getLongArrayAttribute() {
    return longArrayAttribute;
  }

  public long getLongAttribute() {
    return longAttribute;
  }

  public String[] getPasswordArrayAttribute() {
    return passwordArrayAttribute;
  }

  public String getPasswordAttribute() {
    return passwordAttribute;
  }

  public short[] getShortArrayAttribute() {
    return shortArrayAttribute;
  }

  public short getShortAttribute() {
    return shortAttribute;
  }

  public String[] getStringArrayAttribute() {
    return stringArrayAttribute;
  }

  public String getStringAttribute() {
    return stringAttribute;
  }

  public void setBooleanAttribute(final Boolean booleanAttribute) {
    this.booleanAttribute = booleanAttribute;
  }

  public void setByteArrayAttribute(final byte[] byteArrayAttribute) {
    this.byteArrayAttribute = byteArrayAttribute;
  }

  public void setByteAttribute(final byte byteAttribute) {
    this.byteAttribute = byteAttribute;
  }

  public void setCharArrayAttribute(final char[] charArrayAttribute) {
    this.charArrayAttribute = charArrayAttribute;
  }

  public void setCharAttribute(final char charAttribute) {
    this.charAttribute = charAttribute;
  }

  @ServiceRef(configurationType = ReferenceConfigurationType.CLAUSE, optional = true)
  public void setClauseReference(final ServiceHolder<ManagedService> clauseReference) {
  }

  public void setDoubleArrayAttribute(final double[] doubleArrayAttribute) {
    this.doubleArrayAttribute = doubleArrayAttribute;
  }

  public void setDoubleAttribute(final double doubleAttribute) {
    this.doubleAttribute = doubleAttribute;
  }

  public void setFloatArrayAttribute(final float[] floatArrayAttribute) {
    this.floatArrayAttribute = floatArrayAttribute;
  }

  public void setFloatAttribute(final float floatAttribute) {
    this.floatAttribute = floatAttribute;
  }

  @IntegerAttribute
  public void setIntArrayAttribute(final int[] intArrayAttribute) {
    this.intArrayAttribute = intArrayAttribute;
  }

  public void setIntAttribute(final int intAttribute) {
    this.intAttribute = intAttribute;
  }

  public void setLongArrayAttribute(final long[] longArrayAttribute) {
    this.longArrayAttribute = longArrayAttribute;
  }

  public void setLongAttribute(final long longAttribute) {
    this.longAttribute = longAttribute;
  }

  public void setPasswordArrayAttribute(final String[] passwordArrayAttribute) {
    this.passwordArrayAttribute = passwordArrayAttribute;
  }

  public void setPasswordAttribute(final String passwordAttribute) {
    this.passwordAttribute = passwordAttribute;
  }

  public void setShortArrayAttribute(final short[] shortArrayAttribute) {
    this.shortArrayAttribute = shortArrayAttribute;
  }

  public void setShortAttribute(final short shortAttribute) {
    this.shortAttribute = shortAttribute;
  }

  @ServiceRef(dynamic = true)
  public void setSomeReference(final ServiceHolder<ManagedService> someReference) {
  }

  public void setStringArrayAttribute(final String[] stringArrayAttribute) {
    this.stringArrayAttribute = stringArrayAttribute;
  }

  public void setStringAttribute(final String stringAttribute) {
    this.stringAttribute = stringAttribute;
  }
}
