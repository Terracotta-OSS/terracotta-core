/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.monitoring;

import java.util.OptionalLong;

/**
 *
 */
public interface EntityInvokeMetrics {
  /**
   * total invoke count since server start.
   *
   * @return the number of invokes since server start
   */
  long getInvokeCount();
  /**
   * total invoke count since server start for entities of type.
   * @param type full classname of entity type
   * @return OptionalLong if the type exists on the system the value will not be empty
   */
  OptionalLong getInvokeCountForType(String type);
  /**
   * total invoke count since server start for entities of type.
   * @param type full classname of entity type
   * @param name name of entity
   * @return OptionalLong if the id exists on the system the value will not be empty
   */
  OptionalLong getInvokeCountForId(String type, String name);

}
