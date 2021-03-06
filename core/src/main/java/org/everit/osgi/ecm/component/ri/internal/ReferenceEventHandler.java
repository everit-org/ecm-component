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
package org.everit.osgi.ecm.component.ri.internal;

import org.everit.osgi.ecm.component.ri.internal.attribute.ReferenceHelper;
import org.everit.osgi.ecm.metadata.ReferenceMetadata;

/**
 * Event handler that interface that helps {@link ReferenceHelper}s forwarding the events of
 * References to the {@link ComponentContextImpl}.
 */
public interface ReferenceEventHandler {

  void failedDuringConfigurationUpdate(
      ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper);

  void satisfied(ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper);

  void unsatisfied(ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper);

  void updateDynamicWithoutSatisfactionChange(
      ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper);

  void updateNonDynamic(ReferenceHelper<?, ?, ? extends ReferenceMetadata> referenceHelper);
}
