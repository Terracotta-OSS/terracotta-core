/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.GarbageCollectionID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.util.ObjectIDSet;

import java.util.Set;

public class YoungGCHook extends AbstractGCHook {

  private static final TCLogger         logger = TCLogging.getLogger(YoungGCHook.class);
  private final YoungGenChangeCollector youngGenChangeCollector;

  public YoungGCHook(MarkAndSweepGarbageCollector collector, ObjectManager objectManager,
                     ClientStateManager stateManager, YoungGenChangeCollector youngGenChangeCollector) {
    super(collector, objectManager, stateManager);
    this.youngGenChangeCollector = youngGenChangeCollector;
  }

  public String getDescription() {
    return "YoungGen";
  }

  public GarbageCollectionInfo createGCInfo(GarbageCollectionID id) {
    return new GarbageCollectionInfo(id, false);
  }

  public ObjectIDSet getGCCandidates() {
    return (ObjectIDSet) this.youngGenChangeCollector.addYoungGenCandidateObjectIDsTo(new ObjectIDSet());
  }

  public ObjectIDSet getRootObjectIDs(ObjectIDSet candidateIDs) {
    Set idsInMemory = this.objectManager.getObjectIDsInCache();
    idsInMemory.removeAll(candidateIDs);
    Set roots = this.objectManager.getRootIDs();
    Set youngGenRoots = this.youngGenChangeCollector.getRememberedSet();
    youngGenRoots.addAll(roots);
    youngGenRoots.addAll(idsInMemory);
    return new ObjectIDSet(youngGenRoots);
  }

  public Filter getCollectCycleFilter(Set candidateIDs) {
    return new SelectiveFilter(candidateIDs);
  }

  public ObjectIDSet getObjectReferencesFrom(ObjectID id) {
    ManagedObject obj = this.objectManager.getObjectFromCacheByIDOrNull(id);
    if (obj == null) {
      // Not in cache, rescue stage to take care of these inward references.
      return new ObjectIDSet();
    }
    Set references = obj.getObjectReferences();
    this.objectManager.releaseReadOnly(obj);
    return new ObjectIDSet(references);
  }

  public ObjectIDSet getRescueIDs() {
    ObjectIDSet rescueIds = new ObjectIDSet();
    this.stateManager.addAllReferencedIdsTo(rescueIds);
    int stateManagerIds = rescueIds.size();

    this.collector.addNewReferencesTo(rescueIds);
    // Get the new RemeberedSet and rescue that too.
    Set youngGenRoots = this.youngGenChangeCollector.getRememberedSet();
    rescueIds.addAll(youngGenRoots);
    int referenceCollectorIds = rescueIds.size() - stateManagerIds;

    if (logger.isDebugEnabled()) {
      logger.debug("rescueIds: " + rescueIds.size() + ", stateManagerIds: " + stateManagerIds
                   + ", additional referenceCollectorIds: " + referenceCollectorIds);
    }

    return rescueIds;
  }
}