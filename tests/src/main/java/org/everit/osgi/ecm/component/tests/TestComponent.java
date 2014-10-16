package org.everit.osgi.ecm.component.tests;

import java.util.Arrays;

import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Deactivate;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Service
public class TestComponent {

    @StringAttribute
    private String[] stringArrayAttribute;

    @StringAttribute
    private String stringAttribute;

    @Activate
    public void activate() {
        System.out.println("//////////////// activate called: " + stringAttribute + ", "
                + Arrays.toString(stringArrayAttribute));
    }

    @Deactivate
    public void deactivate() {
        System.out.println("---------------- Deactivate called");
    }

    public void setStringArrayAttribute(String[] stringArrayAttribute) {
        this.stringArrayAttribute = stringArrayAttribute;
    }

    public void setStringAttribute(String stringAttribute) {
        this.stringAttribute = stringAttribute;
    }
}
