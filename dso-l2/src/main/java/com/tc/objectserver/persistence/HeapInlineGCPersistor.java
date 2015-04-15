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
package com.tc.objectserver.persistence;

import com.tc.object.ObjectID;
import com.tc.util.BitSetObjectIDSet;

import java.util.Collection;
import java.util.Set;

/**
 * @author tim
 */
public class HeapInlineGCPersistor implements InlineGCPersistor {
  private final Set<ObjectID> set = new BitSetObjectIDSet();

  @Override
  public synchronized int size() {
    return set.size();
  }

  @Override
  public synchronized void addObjectIDs(final Collection<ObjectID> oids) {
    set.addAll(oids);
  }

  @Override
  public synchronized void removeObjectIDs(final Collection<ObjectID> objectIDs) {
    set.removeAll(objectIDs);
  }

  @Override
  public synchronized Set<ObjectID> allObjectIDs() {
    return new BitSetObjectIDSet(set);
  }
}
