/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

import static com.tc.l2.ha.ClusterStateDBKeyNames.STRIPE_KEY_PREFIX;

import com.tc.config.HaConfig;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.net.groups.StripeIDEventListener;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.object.persistence.api.PersistentMapStore;
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
  private final PersistentMapStore                          persistentStateStore;
  private final boolean                                     isAACoordinator;
  private final GroupID                                     thisGroupID;

  public StripeIDStateManagerImpl(HaConfig haConfig, PersistentMapStore persistentStateStore) {
    this.persistentStateStore = persistentStateStore;
    this.isAACoordinator = haConfig.isActiveCoordinatorGroup();
    this.thisGroupID = haConfig.getThisGroupID();
    this.unKnownIDCount.set(loadStripeIDFromDB(haConfig));
  }

  private int loadStripeIDFromDB(HaConfig haConfig) {
    GroupID[] groupIDs = haConfig.getGroupIDs();
    int count = groupIDs.length;
    for (GroupID gid : groupIDs) {
      String id = getFromStore(gid);
      StripeID stripeID;
      if (id != null) {
        stripeID = new StripeID(id);
        --count;
      } else {
        stripeID = StripeID.NULL_ID;
      }
      groupIDToStripeIDMap.put(gid, stripeID);
    }
    return count;
  }

  public boolean isStripeIDMatched(GroupID gid, StripeID stripeID) {
    return !stripeID.isNull() && stripeID.equals(getStripeID(gid));
  }

  public Map<GroupID, StripeID> getStripeIDMap(boolean askMapfromAACoordinator) {
    // return empty map if request map only from AACoordinator and it is not an AACoordinator
    return (!askMapfromAACoordinator || isAACoordinator) ? Collections.unmodifiableMap(groupIDToStripeIDMap)
        : new HashMap<GroupID, StripeID>();
  }

  private String prefixKey(String key) {
    return STRIPE_KEY_PREFIX + key;
  }

  private String getFromStore(GroupID groupID) {
    return persistentStateStore.get(prefixKey(groupID.toString()));
  }

  private void putToStore(GroupID groupID, StripeID stripeID) {
    logger.info("putToStore " + groupID + " " + stripeID);
    groupIDToStripeIDMap.put(groupID, stripeID);
    persistentStateStore.put(prefixKey(groupID.toString()), stripeID.getName());
  }

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
