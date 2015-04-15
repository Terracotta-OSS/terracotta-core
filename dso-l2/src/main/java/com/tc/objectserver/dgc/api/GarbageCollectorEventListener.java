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

public interface GarbageCollectorEventListener {

  public void garbageCollectorStart(GarbageCollectionInfo info);

  public void garbageCollectorMark(GarbageCollectionInfo info);

  public void garbageCollectorMarkResults(GarbageCollectionInfo info);

  public void garbageCollectorRescue1Complete(GarbageCollectionInfo info);

  public void garbageCollectorPausing(GarbageCollectionInfo info);

  public void garbageCollectorPaused(GarbageCollectionInfo info);

  public void garbageCollectorRescue2Start(GarbageCollectionInfo info);

  public void garbageCollectorMarkComplete(GarbageCollectionInfo info);

  public void garbageCollectorCycleCompleted(GarbageCollectionInfo info, ObjectIDSet toDelete);

  public void garbageCollectorCompleted(GarbageCollectionInfo info);

  public void garbageCollectorCanceled(GarbageCollectionInfo info);

}
