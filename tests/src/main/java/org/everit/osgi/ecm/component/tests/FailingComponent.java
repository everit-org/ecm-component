package org.everit.osgi.ecm.component.tests;

import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.BooleanAttribute;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.ConfigurationException;

/**
 * A component to test failures.
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FailingComponent {

  public static final String CONF_FAIL_REFERENCE = "fail_reference";

  public static final String FAIL_DYNAMIC_PROPERTY_SETTER_ATTRIBUTE = "failOnDynamicPropertySetter";

  public static final String FAIL_DYNAMIC_PROPERTY_SETTER_EXCEPTION_MESSAGE =
      "failedOnPropertySetter";

  public static final String FAIL_PROPERTY_SETTER_ATTRIBUTE = "failOnPropertySetter";

  public static final String FAIL_PROPERTY_SETTER_EXCEPTION_MESSAGE = "fail_property_setter";

  /**
   * The activate method fails if failOnActivate property is set to true.
   */
  @Activate
  public void activate(final ComponentContext<FailingComponent> componentContext) {
    Object failOnActivate = componentContext.getProperties().get("failOnActivate");
    if (Boolean.parseBoolean(String.valueOf(failOnActivate))) {
      throw new ConfigurationException("fail_activate");
    }
  }

  /**
   * Setter for reference that causes failing this component.
   *
   * @param failingReference
   *          if the value of the reference is 'fail_reference', the setter will throw an exception.
   */
  @ServiceRef(referenceId = "failingReference", optional = true)
  public void setFailingReference(final String failingReference) {
    if (CONF_FAIL_REFERENCE.equals(failingReference)) {
      throw new ConfigurationException(CONF_FAIL_REFERENCE);
    }
  }

  /**
   * The method throws an exception if failOnDynamicPropertySetter is true.
   *
   * @param failOnDynamicPropertySetter
   *          if true, the method will throw an exception.
   */
  @BooleanAttribute(attributeId = FAIL_PROPERTY_SETTER_ATTRIBUTE, defaultValue = false)
  public void setFailOnDynamicPropertySetter(final boolean failOnDynamicPropertySetter) {
    if (failOnDynamicPropertySetter) {
      throw new ConfigurationException(FAIL_DYNAMIC_PROPERTY_SETTER_EXCEPTION_MESSAGE);
    }
  }

  /**
   * The method throws an exception if failOnPropertySetter is true.
   *
   * @param failOnPropertySetter
   *          if true, the method will throw an exception.
   */
  @BooleanAttribute(attributeId = FAIL_DYNAMIC_PROPERTY_SETTER_ATTRIBUTE, defaultValue = false)
  public void setFailOnPropertySetter(final boolean failOnPropertySetter) {
    if (failOnPropertySetter) {
      throw new ConfigurationException(FAIL_PROPERTY_SETTER_EXCEPTION_MESSAGE);
    }
  }
}
