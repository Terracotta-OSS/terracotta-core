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
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Set;

public class FullGCHook extends AbstractGCHook {

  private static final TCLogger logger                = TCLogging.getLogger(FullGCHook.class);

  private static final long     THROTTLE_GC_MILLIS    = TCPropertiesImpl
                                                          .getProperties()
                                                          .getLong(
                                                                   TCPropertiesConsts.L2_OBJECTMANAGER_DGC_THROTTLE_TIME);

  private static final long     REQUESTS_PER_THROTTLE = TCPropertiesImpl
                                                          .getProperties()
                                                          .getLong(
                                                                   TCPropertiesConsts.L2_OBJECTMANAGER_DGC_REQUEST_PER_THROTTLE);

  private static final Filter   NULL_FILTER           = new Filter() {
                                                        public boolean shouldVisit(ObjectID referencedObject) {
                                                          return true;
                                                        }
                                                      };

  public FullGCHook(MarkAndSweepGarbageCollector collector, ObjectManager objectManager, ClientStateManager stateManager) {
    super(collector, objectManager, stateManager);
  }

  public String getDescription() {
    return "Full";
  }

  public GarbageCollectionInfo createGCInfo(GarbageCollectionID id) {
    return new GarbageCollectionInfo(id, true);
  }

  public ObjectIDSet getGCCandidates() {
    return this.objectManager.getAllObjectIDs();
  }

  public ObjectIDSet getRootObjectIDs(ObjectIDSet candidateIDs) {
    return new ObjectIDSet(this.objectManager.getRootIDs());
  }

  public Filter getCollectCycleFilter(Set candidateIDs) {
    return NULL_FILTER;
  }

  public ObjectIDSet getObjectReferencesFrom(ObjectID id) {
    throttleIfNecessary();
    ManagedObject obj = this.objectManager.getObjectByIDOrNull(id);
    if (obj == null) {
      logger.warn("Looked up a new Object before its initialized, skipping : " + id);
      return new ObjectIDSet();
    }
    Set references = obj.getObjectReferences();
    this.objectManager.releaseReadOnly(obj);
    return new ObjectIDSet(references);
  }

  private int request_count = 0;

  private void throttleIfNecessary() {
    if (THROTTLE_GC_MILLIS > 0 && ++this.request_count % REQUESTS_PER_THROTTLE == 0) {
      ThreadUtil.reallySleep(THROTTLE_GC_MILLIS);
    }
  }

  public ObjectIDSet getRescueIDs() {
    ObjectIDSet rescueIds = new ObjectIDSet();
    this.stateManager.addAllReferencedIdsTo(rescueIds);
    int stateManagerIds = rescueIds.size();

    this.collector.addNewReferencesTo(rescueIds);
    int referenceCollectorIds = rescueIds.size() - stateManagerIds;

    logger.debug("rescueIds: " + rescueIds.size() + ", stateManagerIds: " + stateManagerIds
                 + ", additional referenceCollectorIds: " + referenceCollectorIds);

    return rescueIds;
  }
}