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

import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.util.ObjectIDSet;

import java.io.Serializable;

public abstract class GarbageCollectorEventListenerAdapter implements GarbageCollectorEventListener, Serializable {

  @Override
  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    // do nothing
  }

  @Override
  public void garbageCollectorCycleCompleted(GarbageCollectionInfo info, ObjectIDSet toDelete) {
    //
  }

  @Override
  public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorMark(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorMarkResults(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorPaused(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorPausing(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorRescue1Complete(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorRescue2Start(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorStart(GarbageCollectionInfo info) {
    //
  }

  @Override
  public void garbageCollectorCanceled(GarbageCollectionInfo info) {
    //
  }
}
