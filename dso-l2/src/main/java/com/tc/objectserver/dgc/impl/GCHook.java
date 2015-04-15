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