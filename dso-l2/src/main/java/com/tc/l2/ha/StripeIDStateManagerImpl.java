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
package com.tc.l2.ha;

import com.tc.config.HaConfig;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.net.groups.StripeIDEventListener;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class StripeIDStateManagerImpl implements StripeIDStateManager, PrettyPrintable {
  private static final TCLogger                             logger               = TCLogging
                                                                                     .getLogger(StripeIDStateManagerImpl.class);

  private final CopyOnWriteArrayList<StripeIDEventListener> listeners            = new CopyOnWriteArrayList<StripeIDEventListener>();
  private final Map<GroupID, StripeID>                      groupIDToStripeIDMap = new ConcurrentHashMap<GroupID, StripeID>();
  private final AtomicInteger                               unKnownIDCount       = new AtomicInteger(0);
  private final boolean                                     isAACoordinator;
  private final GroupID                                     thisGroupID;
  private final ClusterStatePersistor                       clusterStatePersistor;

  public StripeIDStateManagerImpl(HaConfig haConfig, ClusterStatePersistor clusterStatePersistor) {
    this.clusterStatePersistor = clusterStatePersistor;
    this.isAACoordinator = haConfig.isActiveCoordinatorGroup();
    this.thisGroupID = haConfig.getThisGroupID();
    this.unKnownIDCount.set(loadStripeIDFromDB(haConfig));
  }

  private int loadStripeIDFromDB(HaConfig haConfig) {
    GroupID[] groupIDs = haConfig.getGroupIDs();
    int count = groupIDs.length;
    for (GroupID gid : groupIDs) {
      StripeID stripeID = clusterStatePersistor.getStripeID(gid);
      if (!stripeID.isNull()) {
        --count;
      }
      groupIDToStripeIDMap.put(gid, stripeID);
    }
    return count;
  }

  @Override
  public boolean isStripeIDMatched(GroupID gid, StripeID stripeID) {
    return !stripeID.isNull() && stripeID.equals(getStripeID(gid));
  }

  @Override
  public Map<GroupID, StripeID> getStripeIDMap(boolean askMapfromAACoordinator) {
    // return empty map if request map only from AACoordinator and it is not an AACoordinator
    return (!askMapfromAACoordinator || isAACoordinator) ? Collections.unmodifiableMap(groupIDToStripeIDMap)
        : new HashMap<GroupID, StripeID>();
  }

  private void putToStore(GroupID groupID, StripeID stripeID) {
    logger.info("putToStore " + groupID + " " + stripeID);
    groupIDToStripeIDMap.put(groupID, stripeID);
    clusterStatePersistor.setStripeID(groupID, stripeID);
  }

  @Override
  public boolean verifyOrSaveStripeID(GroupID gid, StripeID stripeID, boolean overwrite) {
    if (stripeID.isNull()) {
      logger.warn("Ignore null StripeID from " + gid);
      return false;
    }

    StripeID oldID = groupIDToStripeIDMap.get(gid);

    if (oldID == null) {
      logger.warn("non-cluster group " + gid);
      return false;
    }

    if (overwrite || oldID.isNull()) {
      putToStore(gid, stripeID);
      if (oldID.isNull()) unKnownIDCount.decrementAndGet();
      logger.debug("Collected " + gid + " " + stripeID + " count: " + unKnownIDCount.get());

      // if local StripeID ready (by ClusterState), notify listeners
      if (thisGroupID.equals(gid)) {
        notifyLocalStripeIDReady(stripeID);
      }

      if (unKnownIDCount.get() == 0) {
        // notify listeners when have a complete map
        notifyIfStripeIDMapReady();
      }
    } else {
      if (!oldID.equals(stripeID)) {
        logger.error("Mismatch StripeID " + oldID + " with " + stripeID + " on " + gid);
        return false;
      }
    }
    return true;
  }

  @Override
  public StripeID getStripeID(GroupID gid) {
    return groupIDToStripeIDMap.get(gid);
  }

  private void notifyIfStripeIDMapReady() {
    logger.info("Notify StripeIDMap ready");
    for (StripeIDEventListener listener : listeners) {
      listener.notifyStripeIDMapReady();
    }
  }

  private void notifyLocalStripeIDReady(StripeID stripeID) {
    logger.info("Notify local StripeID ready");
    for (StripeIDEventListener listener : listeners) {
      listener.notifyStripeIDCreated(stripeID);
    }
  }

  @Override
  public void registerForStripeIDEvents(StripeIDEventListener listener) {
    listeners.add(listener);

    StripeID stripeID = getStripeID(thisGroupID);
    if (!stripeID.isNull()) {
      listener.notifyStripeIDCreated(stripeID);
    }

    // one case is when fully loaded from DB.
    if (unKnownIDCount.get() == 0) {
      listener.notifyStripeIDMapReady();
    }
  }
  
  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.print("groupIDToStripeIDMap:").flush();
    StringBuilder strBuffer = new StringBuilder();
    for(Iterator<Entry<GroupID, StripeID>> iter = this.groupIDToStripeIDMap.entrySet().iterator(); iter.hasNext();){
      Entry<GroupID, StripeID> entry = iter.next();
      strBuffer.append(entry.getKey() + "->" + entry.getValue()).append(",");
    }
    out.duplicateAndIndent().indent().print(strBuffer.toString()).flush();
    return out;
  }

}
