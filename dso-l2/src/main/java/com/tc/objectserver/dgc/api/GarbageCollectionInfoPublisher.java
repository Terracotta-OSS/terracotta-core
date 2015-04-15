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
package com.tc.objectserver.dgc.api;

import com.tc.util.ObjectIDSet;

public interface GarbageCollectionInfoPublisher {

  public void removeListener(GarbageCollectorEventListener listener);

  public void addListener(GarbageCollectorEventListener listener);

  public void fireGCStartEvent(GarbageCollectionInfo info);

  public void fireGCMarkEvent(GarbageCollectionInfo info);

  public void fireGCMarkResultsEvent(GarbageCollectionInfo info);

  public void fireGCRescue1CompleteEvent(GarbageCollectionInfo info);

  public void fireGCPausingEvent(GarbageCollectionInfo info);

  public void fireGCPausedEvent(GarbageCollectionInfo info);

  public void fireGCRescue2StartEvent(GarbageCollectionInfo info);

  public void fireGCMarkCompleteEvent(GarbageCollectionInfo info);

  public void fireGCCycleCompletedEvent(GarbageCollectionInfo info, ObjectIDSet toDelete);

  public void fireGCCompletedEvent(GarbageCollectionInfo info);

  public void fireGCCanceledEvent(GarbageCollectionInfo info);
}
