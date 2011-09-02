/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.impl.GarbageCollectionID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.ObjectIDSet;

import java.util.Set;

public interface GCHook {

  public ObjectIDSet getGCCandidates();

  public ObjectIDSet getRootObjectIDs(ObjectIDSet candidateIDs);

  public int getLiveObjectCount();

  public GarbageCollectionInfo createGCInfo(GarbageCollectionID id);

  public String getDescription();

  public void startMonitoringReferenceChanges();

  public void stopMonitoringReferenceChanges();

  public Filter getCollectCycleFilter(Set candidateIDs);

  public void waitUntilReadyToGC();

  public Set<ObjectID> getObjectReferencesFrom(ObjectID id);

  public ObjectIDSet getRescueIDs();

}