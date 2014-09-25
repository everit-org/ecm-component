package org.everit.osgi.ecm.component.context;

import java.util.Dictionary;

public interface ComponentContext {

    Dictionary<String, Object> getProperties();

    void getRequirements();
}
