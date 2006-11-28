/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util.sequence;

public class ObjectIDSequenceProvider implements ObjectIDSequence {
  
  private long current;

  public ObjectIDSequenceProvider(long start) {
    this.current = start;
  }

  public synchronized long nextObjectIDBatch(int batchSize) {
    final long start = current;
    current += batchSize;
    return start; 
  }

}
