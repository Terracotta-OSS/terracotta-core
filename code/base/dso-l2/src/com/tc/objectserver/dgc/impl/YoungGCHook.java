/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.util.ObjectIDSet;

import java.util.Set;

public class YoungGCHook extends AbstractGCHook {

  private static final TCLogger         logger = TCLogging.getLogger(YoungGCHook.class);

  public YoungGCHook(GarbageCollector collector, ObjectManager objectManager,
                     ClientStateManager stateManager) {
    super(collector, objectManager, stateManager);
  }

  public String getDescription() {
    return "YoungGen";
  }

  public GarbageCollectionInfo getGCInfo(int gcIteration) {
    return new GarbageCollectionInfo(gcIteration, false);
  }

  public ObjectIDSet getGCCandidates() {
    return (ObjectIDSet) collector.getYoungGenChangeCollector().addYoungGenCandidateObjectIDsTo(new ObjectIDSet());
  }

  public ObjectIDSet getRootObjectIDs(ObjectIDSet candidateIDs) {
    Set idsInMemory = objectManager.getObjectIDsInCache();
    idsInMemory.removeAll(candidateIDs);
    Set roots = objectManager.getRootIDs();
    Set youngGenRoots = collector.getYoungGenChangeCollector().getRememberedSet();
    youngGenRoots.addAll(roots);
    youngGenRoots.addAll(idsInMemory);
    return new ObjectIDSet(youngGenRoots);
  }

  public Filter getCollectCycleFilter(Set candidateIDs) {
    return new SelectiveFilter(candidateIDs);
  }

  public ObjectIDSet getObjectReferencesFrom(ObjectID id) {
    ManagedObject obj = objectManager.getObjectFromCacheByIDOrNull(id);
    if (obj == null) {
      // Not in cache, rescue stage to take care of these inward references.
      return new ObjectIDSet();
    }
    Set references = obj.getObjectReferences();
    objectManager.releaseReadOnly(obj);
    return new ObjectIDSet(references);
  }

  public ObjectIDSet getRescueIDs() {
    ObjectIDSet rescueIds = new ObjectIDSet();
    stateManager.addAllReferencedIdsTo(rescueIds);
    int stateManagerIds = rescueIds.size();

    collector.addNewReferencesTo(rescueIds);
    // Get the new RemeberedSet and rescue that too.
    Set youngGenRoots = collector.getYoungGenChangeCollector().getRememberedSet();
    rescueIds.addAll(youngGenRoots);
    int referenceCollectorIds = rescueIds.size() - stateManagerIds;

    logger.debug("rescueIds: " + rescueIds.size() + ", stateManagerIds: " + stateManagerIds
                 + ", additional referenceCollectorIds: " + referenceCollectorIds);

    return rescueIds;
  }

  public void notifyGCComplete(GCResultContext gcResult) {
    objectManager.notifyGCComplete(gcResult);
  }

}