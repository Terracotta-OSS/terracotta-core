/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.ObjectIDSet;

import java.util.Set;

interface GCHook {

  public ObjectIDSet getGCCandidates();

  public Set getRootObjectIDs(ObjectIDSet candidateIDs);

  public GarbageCollectionInfo getGCInfo(int gcIteration);

  public String getDescription();

  public void startMonitoringReferenceChanges();

  public void stopMonitoringReferenceChanges();

  public Filter getCollectCycleFilter(Set candidateIDs);

  public void waitUntilReadyToGC();

  public Set getObjectReferencesFrom(ObjectID id);

  public Set getRescueIDs();
}