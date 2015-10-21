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
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

package com.tc.object.locks;

import com.tc.net.GroupID;
import com.tc.net.OrderedGroupIDs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class EnterpriseLockDistributionStrategy implements LockDistributionStrategy {
  private final GroupID[] groupIDs;

  public EnterpriseLockDistributionStrategy(OrderedGroupIDs orderedGroupIDs) {
    this.groupIDs = orderedGroupIDs.getGroupIDs();
  }

  @Override
  public GroupID getGroupIDFor(LockID lock) {
    int hash = hash(lock.hashCode());
    return this.groupIDs[Math.abs(hash % this.groupIDs.length)];
  }

  public SortedMap<GroupID, List<Notify>> segregateNotifies2Groups(List<Notify> notifies) {
    TreeMap<GroupID, List<Notify>> groupIds2Notifies = new TreeMap<GroupID, List<Notify>>();
    for (Iterator<Notify> notifyIterator = notifies.iterator(); notifyIterator.hasNext();) {
      Notify notify = notifyIterator.next();
      GroupID gid = getGroupIDFor(notify.getLockID());
      List<Notify> list = groupIds2Notifies.get(gid);
      if (list == null) {
        list = new ArrayList<Notify>();
        groupIds2Notifies.put(gid, list);
      }
      list.add(notify);
    }
    return groupIds2Notifies;
  }

  /**
   * Applies a supplemental hash function to a given hashCode, which defends against poor quality hash functions.
   */
  private static int hash(int h) {
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }
}
