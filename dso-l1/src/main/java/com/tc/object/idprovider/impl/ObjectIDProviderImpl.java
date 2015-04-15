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
package com.tc.object.idprovider.impl;

import com.tc.net.GroupID;
import com.tc.object.ObjectID;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.tx.ClientTransaction;
import com.tc.util.sequence.Sequence;

import java.util.SortedSet;
import java.util.TreeSet;

public class ObjectIDProviderImpl implements ObjectIDProvider {

  private final Sequence        sequence;
  private final SortedSet<Long> cachedObjectIds = new TreeSet<Long>();

  public ObjectIDProviderImpl(Sequence sequence) {
    this.sequence = sequence;
  }

  @Override
  public synchronized ObjectID next(ClientTransaction txn, Object pojo, GroupID gid) {
    long oidLong = -1;
    if (cachedObjectIds.size() > 0) {
      oidLong = this.cachedObjectIds.first();
      this.cachedObjectIds.remove(oidLong);
    } else {
      oidLong = this.sequence.next();
    }

    return new ObjectID(oidLong);
  }

  @Override
  public synchronized void reserve(int size, GroupID gid) {
    int sizeNeeded = size - cachedObjectIds.size();
    for (int i = 0; i < sizeNeeded; i++) {
      cachedObjectIds.add(this.sequence.next());
    }
  }
}