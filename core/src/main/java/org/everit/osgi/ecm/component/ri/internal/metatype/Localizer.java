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
package org.everit.osgi.ecm.component.ri.internal.metatype;

import java.util.ResourceBundle;

/**
 * The localizer helps getting localized value for labels starting with %.
 */
public class Localizer {

  private final ResourceBundle resourceBundle;

  public Localizer(final ResourceBundle resourceBundle) {
    this.resourceBundle = resourceBundle;
  }

  /**
   * Translates the text to a specified language.
   *
   * @param text
   *          The text that should be translated.
   * @return The translated text.
   */
  public String localize(final String text) {
    if (text == null) {
      return null;
    }

    if (!text.startsWith("%")) {
      return text;
    }

    String key = text.substring(1);

    if ((key.length() == 0) || (resourceBundle == null) || !resourceBundle.containsKey(key)) {
      return key;
    }
    return resourceBundle.getString(key);
  }
}
