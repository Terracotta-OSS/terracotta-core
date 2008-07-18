/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

public interface GCStats {
  
  int getIteration();
  
  String getType();

  String getStatus();
  
  long getStartTime(); 

  long getElapsedTime();

  long getBeginObjectCount();

  long getCandidateGarbageCount();

  long getActualGarbageCount();
  
  long getMarkStageTime();
  
  long getPausedStageTime();
  
  long getDeleteStageTime();

}
