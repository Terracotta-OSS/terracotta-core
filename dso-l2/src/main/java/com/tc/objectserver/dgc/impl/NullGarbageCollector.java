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
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.LifeCycleState;

public class NullGarbageCollector implements GarbageCollector {

  @Override
  public boolean isPausingOrPaused() {
    return false;
  }

  @Override
  public boolean isPaused() {
    return false;
  }

  @Override
  public void notifyReadyToGC() {
    return;
  }

  @Override
  public void requestGCPause() {
    return;
  }

  @Override
  public void notifyGCComplete() {
    return;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out.print(getClass().getName());
  }

  @Override
  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    // do nothing null
  }

  @Override
  public void doGC(GCType type) {
    //
  }

  @Override
  public void start() {
    // do nothing null
  }

  @Override
  public void stop() {
    // do nothing null
  }

  @Override
  public void setState(LifeCycleState st) {
    // do nothing null
  }

  @Override
  public void addListener(GarbageCollectorEventListener listener) {
    //
  }

  @Override
  public boolean requestDisableGC() {
    return true;
  }

  @Override
  public void enableGC() {
    // do nothing null
  }

  @Override
  public void waitToDisableGC() {
    // do nothing
  }

  @Override
  public boolean isDisabled() {
    return true;
  }

  @Override
  public boolean isStarted() {
    return false;
  }

  @Override
  public void deleteGarbage(DGCResultContext resultContext) {
    //
  }

  @Override
  public boolean requestGCStart() {
    return false;
  }

  @Override
  public void waitToStartGC() {
    // do nothing
  }

  @Override
  public void waitToStartInlineGC() {
    // do nothing
  }

  @Override
  public void setPeriodicEnabled(boolean periodicEnabled) {
    // do nothing
  }

  @Override
  public boolean isPeriodicEnabled() {
    return false;
  }

  @Override
  public boolean isDelete() {
    return false;
  }

  @Override
  public boolean requestGCDeleteStart() {
    return false;
  }
}
