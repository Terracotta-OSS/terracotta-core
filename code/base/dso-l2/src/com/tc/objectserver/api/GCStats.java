/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.api;

public interface GCStats {
  
  int getIteration();

  long getStartTime();

  long getElapsedTime();

  long getBeginObjectCount();

  long getCandidateGarbageCount();

  long getActualGarbageCount();

}
