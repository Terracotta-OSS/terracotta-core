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
package com.tc.objectserver.api;

import com.tc.util.State;

public interface GCStats {
  public static final State GC_START         = new State("START");
  public static final State GC_MARK          = new State("MARK");
  public static final State GC_PAUSE         = new State("PAUSE");
  public static final State GC_MARK_COMPLETE = new State("MARK_COMPLETE");
  public static final State GC_COMPLETE      = new State("COMPLETE");
  public static final State GC_CANCELED      = new State("CANCELED");

  int getIteration();

  String getType();

  String getStatus();

  long getStartTime();

  long getElapsedTime();

  long getBeginObjectCount();

  long getEndObjectCount();

  long getCandidateGarbageCount();

  long getActualGarbageCount();

  long getMarkStageTime();

  long getPausedStageTime();

}
