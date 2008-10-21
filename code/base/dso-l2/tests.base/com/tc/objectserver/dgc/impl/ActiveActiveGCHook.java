/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.impl.ObjectManagerImpl;
import com.tc.util.ObjectIDSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class ActiveActiveGCHook implements GCHook {

  private final ObjectManagerArray              objectManagerArray;
  private final MarkAndSweepGarbageCollector    collector;

  private static final Filter                   NULL_FILTER               = new Filter() {
                                                                            public boolean shouldVisit(
                                                                                                       ObjectID referencedObject) {
                                                                              return true;
                                                                            }
                                                                          };

  private final Map<ObjectManager, ObjectIDSet> objectManager2ObjectIDSet = new HashMap<ObjectManager, ObjectIDSet>();

  public ActiveActiveGCHook(ObjectManagerArray objectManagerArray, MarkAndSweepGarbageCollector collector) {
    this.objectManagerArray = objectManagerArray;
    this.collector = collector;
  }

  public Filter getCollectCycleFilter(Set candidateIDs) {
    return NULL_FILTER;
  }

  public String getDescription() {
    return "Active-Active Full";
  }

  public ObjectIDSet getGCCandidates() {
    ObjectIDSet allObjectIDs = new ObjectIDSet();
    ObjectManagerImpl[] objectManagers = objectManagerArray.getObjectManagers();
    for (int i = 0; i < objectManagers.length; i++) {
      ObjectIDSet objectIDs = objectManagers[i].getAllObjectIDs();
      objectManager2ObjectIDSet.put(objectManagers[i], objectIDs);
      allObjectIDs.addAll(objectIDs);
    }
    return allObjectIDs;
  }

  public GarbageCollectionInfo getGCInfo(int gcIteration) {
    return new GarbageCollectionInfo(gcIteration, true);
  }

  public Set getObjectReferencesFrom(ObjectID id) {
    for (Iterator<Entry<ObjectManager, ObjectIDSet>> iter = objectManager2ObjectIDSet.entrySet().iterator(); iter
        .hasNext();) {
      Entry<ObjectManager, ObjectIDSet> entry = iter.next();
      if (entry.getValue().contains(id)) {
        ObjectManager objectManager = entry.getKey();
        ManagedObject obj = objectManager.getObjectByIDOrNull(id);
        if (obj == null) { return Collections.EMPTY_SET; }
        Set references = obj.getObjectReferences();
        objectManager.releaseReadOnly(obj);
        return references;
      }
    }
    return Collections.EMPTY_SET;
  }

  public Set getRescueIDs() {
    Set rescueIds = new ObjectIDSet();

    ObjectManagerImpl[] objectManagers = objectManagerArray.getObjectManagers();
    for (int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].getClientStateManager().addAllReferencedIdsTo(rescueIds);
    }

    collector.addNewReferencesTo(rescueIds);

    return rescueIds;
  }

  public Set getRootObjectIDs(ObjectIDSet candidateIDs) {
    ObjectIDSet allRoots = new ObjectIDSet();
    ObjectManagerImpl[] objectManagers = objectManagerArray.getObjectManagers();
    for (int i = 0; i < objectManagers.length; i++) {
      allRoots.addAll(objectManagers[i].getRootIDs());
    }
    return allRoots;
  }

  public void startMonitoringReferenceChanges() {
    collector.startMonitoringReferenceChanges();
  }

  public void stopMonitoringReferenceChanges() {
    collector.stopMonitoringReferenceChanges();
  }

  public void waitUntilReadyToGC() {
    ObjectManagerImpl[] objectManagers = objectManagerArray.getObjectManagers();
    for (int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].waitUntilReadyToGC();
    }
  }

}
