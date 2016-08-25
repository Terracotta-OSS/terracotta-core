/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


/**
 */
public class PassthroughReferenceManager {
  private final Map<Long, PassthroughEntityTuple> references;

  public PassthroughReferenceManager() {
    this.references = new HashMap<Long, PassthroughEntityTuple>();
  }


  public synchronized void reference(PassthroughEntityTuple entityTuple, long clientOriginID) {
    references.put(clientOriginID, entityTuple);
  }

  public synchronized boolean drop(PassthroughEntityTuple entityTuple, long clientOriginID) {
    return references.remove(clientOriginID, entityTuple);
  }
  
  public synchronized boolean isReferenced(PassthroughEntityTuple entityTuple) {
    return references.values().contains(entityTuple);
  }
}
