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
package com.tc.objectserver.gtx;

import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class ServerTransactionIDBookKeeper {

  private final static TreeMap EMPTY_TREEMAP = new TreeMap();

  private final HashMap        nodes         = new HashMap();

  public synchronized GlobalTransactionDescriptor add(ServerTransactionID sid, GlobalTransactionDescriptor gtx) {
    TreeMap tids = getOrCreateTreeMap(sid.getSourceID());
    return (GlobalTransactionDescriptor) tids.put(sid.getClientTransactionID(), gtx);
  }

  private TreeMap getOrCreateTreeMap(NodeID nodeID) {
    TreeMap tm = (TreeMap) nodes.get(nodeID);
    if (tm == null) {
      tm = new TreeMap();
      nodes.put(nodeID, tm);
    }
    return tm;
  }

  private TreeMap getTreeMap(NodeID nodeID) {
    TreeMap tm = (TreeMap) nodes.get(nodeID);
    return (tm == null ? EMPTY_TREEMAP : tm);
  }

  public synchronized GlobalTransactionDescriptor get(ServerTransactionID sid) {
    TreeMap tids = getTreeMap(sid.getSourceID());
    return (GlobalTransactionDescriptor) tids.get(sid.getClientTransactionID());
  }

  public synchronized GlobalTransactionDescriptor remove(ServerTransactionID sid) {
    TreeMap tids = getTreeMap(sid.getSourceID());
    return (GlobalTransactionDescriptor) tids.remove(sid.getClientTransactionID());
  }

  public synchronized Collection removeAll(NodeID nid) {
    TreeMap tm = (TreeMap) nodes.remove(nid);
    if (tm != null) {
      return tm.values();
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  public synchronized Collection removeAllExcept(Set cids) {
    List removed = new ArrayList();
    for (Iterator i = nodes.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Entry) i.next();
      if (!cids.contains(e.getKey())) {
        TreeMap tm = (TreeMap) e.getValue();
        removed.addAll(tm.values());
        i.remove();
      }
    }
    return removed;
  }

  public synchronized Collection<GlobalTransactionDescriptor> clearCommitedSidsBelowLowWaterMark(ServerTransactionID sid) {
    List removed = new ArrayList();
    TreeMap tids = getTreeMap(sid.getSourceID());
    Map toRemove = tids.headMap(sid.getClientTransactionID());
    for (Iterator i = toRemove.values().iterator(); i.hasNext();) {
      GlobalTransactionDescriptor gd = (GlobalTransactionDescriptor) i.next();
      // We are only removing the transactions that are already committed to disk. Transactions could be reordered in
      // the passive, so we need this check.
      if (gd.complete()) {
        i.remove();
        removed.add(gd);
      }
    }
    return Collections.unmodifiableCollection(removed);
  }

}
