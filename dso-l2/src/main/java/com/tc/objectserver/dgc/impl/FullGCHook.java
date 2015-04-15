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
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.impl.GarbageCollectionID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.util.Set;

public class FullGCHook extends AbstractGCHook {

  private static final TCLogger logger      = TCLogging.getLogger(FullGCHook.class);

  private static final Filter   NULL_FILTER = new Filter() {
                                              @Override
                                              public boolean shouldVisit(ObjectID referencedObject) {
                                                return true;
                                              }
                                            };

  public FullGCHook(MarkAndSweepGarbageCollector collector, ObjectManager objectManager,
                    ClientStateManager stateManager, boolean inlineCleanup) {
    super(collector, objectManager, stateManager, inlineCleanup);
  }

  @Override
  public String getDescription() {
    return "Full";
  }

  @Override
  public GarbageCollectionInfo createGCInfo(GarbageCollectionID id) {
    return new GarbageCollectionInfo(id, inlineCleanup ? GarbageCollectionInfo.Type.INLINE_CLEANUP
        : GarbageCollectionInfo.Type.FULL_GC);
  }

  @Override
  public ObjectIDSet getGCCandidates() {
    return this.objectManager.getAllObjectIDs();
  }

  @Override
  public ObjectIDSet getRootObjectIDs(ObjectIDSet candidateIDs) {
    return new BitSetObjectIDSet(this.objectManager.getRootIDs());
  }

  @Override
  public Filter getCollectCycleFilter(Set candidateIDs) {
    return NULL_FILTER;
  }

  @Override
  public Set<ObjectID> getObjectReferencesFrom(ObjectID id) {
    return getObjectReferencesFrom(id, false);
  }

  @Override
  public ObjectIDSet getRescueIDs() {
    ObjectIDSet rescueIds = new BitSetObjectIDSet();
    this.stateManager.addAllReferencedIdsTo(rescueIds);
    int stateManagerIds = rescueIds.size();

    this.collector.addNewReferencesTo(rescueIds);
    int referenceCollectorIds = rescueIds.size() - stateManagerIds;

    logger.debug("rescueIds: " + rescueIds.size() + ", stateManagerIds: " + stateManagerIds
                 + ", additional referenceCollectorIds: " + referenceCollectorIds);

    return rescueIds;
  }
}