package org.everit.osgi.ecm.component.tests;

import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Service
public class TestComponent {

    @StringAttribute(multiple = false)
    private String[] stringArrayAttribute;

    @StringAttribute
    private String stringAttribute;

    public void setStringArrayAttribute(String[] stringArrayAttribute) {
        this.stringArrayAttribute = stringArrayAttribute;
    }

    public void setStringAttribute(String stringAttribute) {
        this.stringAttribute = stringAttribute;
    }
}
