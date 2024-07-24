/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.passthrough;

import java.util.HashMap;
import java.util.Map;


public class PassthroughReferenceManager {
  private final Map<Long, PassthroughEntityTuple> references;

  public PassthroughReferenceManager() {
    this.references = new HashMap<Long, PassthroughEntityTuple>();
  }


  public synchronized void reference(PassthroughEntityTuple entityTuple, long clientOriginID) {
    references.put(clientOriginID, entityTuple);
  }

  public synchronized boolean drop(PassthroughEntityTuple entityTuple, long clientOriginID) {
    boolean didRemove = false;
    if (entityTuple.equals(references.get(clientOriginID))) {
      references.remove(clientOriginID);
      didRemove = true;
    }
    return didRemove;
  }
  
  public synchronized boolean isReferenced(PassthroughEntityTuple entityTuple) {
    return references.values().contains(entityTuple);
  }
}
