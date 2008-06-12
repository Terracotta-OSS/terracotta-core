/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

public interface GCStats {
  
  int getIteration();

  long getStartTime();

  long getElapsedTime();

  long getBeginObjectCount();

  long getCandidateGarbageCount();

  long getActualGarbageCount();

  long getPausedTime();
  
  long getDeleteTime();

}
