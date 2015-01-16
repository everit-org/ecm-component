package org.everit.osgi.ecm.component.tests;

import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class WrongActivationMethodComponent {

    @Activate
    public void acivate(final Thread someWrongArgument) {
    }
}
