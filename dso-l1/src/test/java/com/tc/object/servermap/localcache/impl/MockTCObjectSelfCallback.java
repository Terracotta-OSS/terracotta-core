/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
import com.tc.util.BitSetObjectIDSet;

import java.util.Set;

public class MockTCObjectSelfCallback implements TCObjectSelfCallback {
  private final Set<ObjectID> oids = new BitSetObjectIDSet();

  @Override
  public void initializeTCClazzIfRequired(TCObjectSelf tcoObjectSelf) {
    // NO OP
    // We do not have tc class factory here
  }

  @Override
  public synchronized void removedTCObjectSelfFromStore(TCObjectSelf tcoObjectSelf) {
    oids.add(tcoObjectSelf.getObjectID());
  }

  public synchronized Set<ObjectID> getRemovedSet() {
    return oids;
  }

  public void removedTCObjectSelfFromStore(ObjectID objectID) {
    oids.add(objectID);
  }
}