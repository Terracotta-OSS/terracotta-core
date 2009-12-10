/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.util.State;

public interface GCStats {

  public static final State GC_START         = new State("START");
  public static final State GC_MARK          = new State("MARK");
  public static final State GC_PAUSE         = new State("PAUSE");
  public static final State GC_MARK_COMPLETE = new State("MARK_COMPLETE");
  public static final State GC_DELETE        = new State("DELETE");
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

  long getDeleteStageTime();

}
